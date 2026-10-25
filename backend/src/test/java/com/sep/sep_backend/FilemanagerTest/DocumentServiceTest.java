package com.sep.sep_backend.FilemanagerTest;

import com.sep.sep_backend.Filemanager.Document;
import com.sep.sep_backend.Filemanager.DocumentRepository;
import com.sep.sep_backend.Filemanager.DocumentService;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TravelEntryRepository travelEntryRepository;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDocumentsByTrip_returnsDocumentsFromRepository() {
        UUID travelEntryId = UUID.randomUUID();
        Document doc = new Document();
        doc.setName("Ticket.pdf");
        when(documentRepository.findByTravelEntryId(travelEntryId)).thenReturn(List.of(doc));

        List<Document> result = documentService.getDocumentsByTrip(travelEntryId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Ticket.pdf");
    }

    @Test
    void saveDocument_validTrip_uploadsToR2AndPersistsMetadata() throws IOException {
        // 1. GIVEN
        UUID travelEntryId = UUID.randomUUID();

        TravelEntry travelEntry = new TravelEntry();
        travelEntry.setId(travelEntryId);
        travelEntry.setUserId("user-1");

        MockMultipartFile file = new MockMultipartFile(
                "file", "Ticket.pdf", "application/pdf", "dummy-content".getBytes());

        when(travelEntryRepository.findById(travelEntryId)).thenReturn(Optional.of(travelEntry));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

        // 2. WHEN
        Document result = documentService.saveDocument(travelEntryId, file);

        // 3. THEN
        assertThat(result.getName()).isEqualTo("Ticket.pdf");
        assertThat(result.getFileType()).isEqualTo("application/pdf");
        assertThat(result.getFileUrl()).startsWith("user-1/");
        assertThat(result.getFileUrl()).endsWith("_Ticket.pdf");
        assertThat(result.getTravelEntry()).isEqualTo(travelEntry);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void saveDocument_invalidTrip_throwsException() {
        UUID travelEntryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "Ticket.pdf", "application/pdf", "dummy-content".getBytes());

        when(travelEntryRepository.findById(travelEntryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.saveDocument(travelEntryId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reiseeintrag nicht gefunden");

        verifyNoInteractions(s3Client);
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void getDocumentStream_requestsObjectWithGivenKey() {
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> stream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream);

        ResponseInputStream<GetObjectResponse> result = documentService.getDocumentStream("user-1/uuid_Ticket.pdf");

        assertThat(result).isSameAs(stream);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("user-1/uuid_Ticket.pdf");
    }

    @Test
    void deleteDocument_existingId_deletesFromR2AndDatabase() {
        UUID documentId = UUID.randomUUID();
        Document doc = new Document();
        doc.setId(documentId);
        doc.setFileUrl("user-1/uuid_Ticket.pdf");

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(documentId);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        verify(documentRepository).delete(doc);
    }

    @Test
    void deleteDocument_nonExistingId_throwsException() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(documentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dokument nicht gefunden");

        verifyNoInteractions(s3Client);
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void getDocumentById_existingId_returnsDocument() {
        UUID documentId = UUID.randomUUID();
        Document doc = new Document();
        doc.setId(documentId);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        Document result = documentService.getDocumentById(documentId);

        assertThat(result).isEqualTo(doc);
    }

    @Test
    void getDocumentById_nonExistingId_throwsException() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById(documentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dokument mit ID " + documentId + " wurde nicht gefunden.");
    }
}
