package com.sep.sep_backend.Filemanager;

import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Stores and retrieves trip documents in Cloudflare R2 and their metadata in the database.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TravelEntryRepository travelEntryRepository;
    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    public DocumentService(DocumentRepository documentRepository,
                           TravelEntryRepository travelEntryRepository,
                           S3Client s3Client) {
        this.documentRepository = documentRepository;
        this.travelEntryRepository = travelEntryRepository;
        this.s3Client = s3Client;
    }

    /**
     * Returns all documents belonging to the given travel entry.
     *
     * @param travelEntryId id of the travel entry
     * @return the documents of that trip
     */
    public List<Document> getDocumentsByTrip(UUID travelEntryId) {
        return documentRepository.findByTravelEntryId(travelEntryId);
    }

    /**
     * Uploads a file to Cloudflare R2 and saves its metadata.
     *
     * @param travelEntryId id of the travel entry the file belongs to
     * @param file          the file to upload
     * @return the saved document
     * @throws IOException if reading the file fails
     */
    public Document saveDocument(UUID travelEntryId, MultipartFile file) throws IOException {
        TravelEntry travelEntry = travelEntryRepository.findById(travelEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Reiseeintrag nicht gefunden"));

        String userId = travelEntry.getUserId() != null ? travelEntry.getUserId() : "unknown_user";

        // Key structure in the bucket: userId/uuid_filename
        String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String r2ObjectKey = userId + "/" + uniqueFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(r2ObjectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFileUrl(r2ObjectKey);
        document.setTravelEntry(travelEntry);

        return documentRepository.save(document);
    }

    /**
     * Opens a stream for a file in the private Cloudflare R2 bucket.
     *
     * @param r2ObjectKey the object key inside the bucket
     * @return the file content stream
     */
    public ResponseInputStream<GetObjectResponse> getDocumentStream(String r2ObjectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(r2ObjectKey)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    /**
     * Deletes a document from Cloudflare R2 and removes its database record.
     *
     * @param documentId id of the document
     */
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Dokument nicht gefunden"));

        String r2ObjectKey = document.getFileUrl();

        // The database record is removed even if the R2 delete fails.
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(r2ObjectKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            System.err.println("Cloudflare R2 Datei konnte nicht gelöscht werden: " + e.getMessage());
        }

        documentRepository.delete(document);
    }

    /**
     * Returns a document by its id.
     *
     * @param documentId id of the document
     * @return the document
     */
    public Document getDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Dokument mit ID " + documentId + " wurde nicht gefunden."));
    }
}
