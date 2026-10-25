package com.sep.sep_backend.FilemanagerTest;

import com.sep.sep_backend.Filemanager.Document;
import com.sep.sep_backend.Filemanager.DocumentController;
import com.sep.sep_backend.Filemanager.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController controller;

    // --- GET /api/trips/{travelEntryId}/documents ---

    @Test
    void getDocuments_shouldReturn200WithList() {
        UUID travelEntryId = UUID.randomUUID();
        Document doc = new Document();
        when(documentService.getDocumentsByTrip(travelEntryId)).thenReturn(List.of(doc));

        ResponseEntity<List<Document>> response = controller.getDocuments(travelEntryId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    // --- POST /api/trips/{travelEntryId}/documents ---

    @Test
    void uploadDocument_shouldReturn201() throws Exception {
        UUID travelEntryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "Ticket.pdf", "application/pdf", "dummy-content".getBytes());
        Document saved = new Document();

        when(documentService.saveDocument(eq(travelEntryId), any(MultipartFile.class))).thenReturn(saved);

        ResponseEntity<?> response = controller.uploadDocument(travelEntryId, file);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(saved, response.getBody());
    }

    @Test
    void uploadDocument_emptyFile_shouldReturn400() {
        UUID travelEntryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        ResponseEntity<?> response = controller.uploadDocument(travelEntryId, file);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void uploadDocument_shouldReturn404_whenIllegalArgumentException() throws Exception {
        UUID travelEntryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "Ticket.pdf", "application/pdf", "dummy-content".getBytes());

        when(documentService.saveDocument(eq(travelEntryId), any(MultipartFile.class)))
                .thenThrow(new IllegalArgumentException("Reiseeintrag nicht gefunden"));

        ResponseEntity<?> response = controller.uploadDocument(travelEntryId, file);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Reiseeintrag nicht gefunden", response.getBody());
    }

    // --- DELETE /api/trips/documents/{documentId} ---

    @Test
    void deleteDocument_shouldReturn204() {
        UUID documentId = UUID.randomUUID();

        ResponseEntity<?> response = controller.deleteDocument(documentId);

        assertEquals(204, response.getStatusCode().value());
        verify(documentService).deleteDocument(documentId);
    }

    @Test
    void deleteDocument_shouldReturn404_whenIllegalArgumentException() {
        UUID documentId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Dokument nicht gefunden"))
                .when(documentService).deleteDocument(documentId);

        ResponseEntity<?> response = controller.deleteDocument(documentId);

        assertEquals(404, response.getStatusCode().value());
    }

    // --- GET /api/trips/documents/download/{documentId} ---

    @Test
    void downloadDocument_shouldReturn200WithFileType() {
        UUID documentId = UUID.randomUUID();
        Document doc = new Document();
        doc.setName("Ticket.pdf");
        doc.setFileType("application/pdf");
        doc.setFileUrl("user-1/uuid_Ticket.pdf");

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> stream = mock(ResponseInputStream.class);

        when(documentService.getDocumentById(documentId)).thenReturn(doc);
        when(documentService.getDocumentStream("user-1/uuid_Ticket.pdf")).thenReturn(stream);

        ResponseEntity<Resource> response = controller.downloadDocument(documentId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
    }

    @Test
    void downloadDocument_shouldReturn404_whenIllegalArgumentException() {
        UUID documentId = UUID.randomUUID();
        when(documentService.getDocumentById(documentId))
                .thenThrow(new IllegalArgumentException("Dokument nicht gefunden"));

        ResponseEntity<Resource> response = controller.downloadDocument(documentId);

        assertEquals(404, response.getStatusCode().value());
    }
}
