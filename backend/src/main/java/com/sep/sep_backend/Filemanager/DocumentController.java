package com.sep.sep_backend.Filemanager;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing documents attached to travel entries.
 */
@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Returns all documents belonging to a travel entry.
     *
     * @param travelEntryId id of the travel entry
     * @return the documents of that trip
     */
    @GetMapping("/{travelEntryId}/documents")
    public ResponseEntity<List<Document>> getDocuments(@PathVariable UUID travelEntryId) {
        return ResponseEntity.ok(documentService.getDocumentsByTrip(travelEntryId));
    }

    /**
     * Uploads a document for a travel entry and stores it in Cloudflare R2.
     *
     * @param travelEntryId id of the travel entry
     * @param file          the file to upload
     * @return the saved document, or an error response
     */
    @PostMapping("/{travelEntryId}/documents")
    public ResponseEntity<?> uploadDocument(
            @PathVariable UUID travelEntryId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Datei darf nicht leer sein.");
            }

            Document savedDocument = documentService.saveDocument(travelEntryId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDocument);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Hochladen des Dokuments: " + e.getMessage());
        }
    }

    /**
     * Deletes a document from the database and from Cloudflare R2.
     *
     * @param documentId id of the document
     * @return 204 on success, or an error response
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Löschen des Dokuments: " + e.getMessage());
        }
    }

    /**
     * Streams a file from the private Cloudflare R2 bucket to the client.
     *
     * @param documentId id of the document
     * @return the file content as a stream
     */
    @GetMapping("/documents/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID documentId) {
        try {
            Document document = documentService.getDocumentById(documentId);
            var r2Stream = documentService.getDocumentStream(document.getFileUrl());
            InputStreamResource resource = new InputStreamResource(r2Stream);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getName() + "\"")
                    .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
