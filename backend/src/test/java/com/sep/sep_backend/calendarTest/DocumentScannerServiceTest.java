package com.sep.sep_backend.calendarTest;

import com.sep.sep_backend.calendar.DocumentScannerService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentScannerServiceTest {

    @Mock private RestTemplate restTemplate;
    @InjectMocks private DocumentScannerService documentScannerService;

    // The @Value fields are not populated outside a Spring context.
    @BeforeEach
    void setUpApiConfig() {
        ReflectionTestUtils.setField(documentScannerService, "groqApiKey", "test-key");
        ReflectionTestUtils.setField(documentScannerService, "groqUrl", "https://groq.test/chat/completions");
    }

    private byte[] createTestPdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(100, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    public void testScanAndExtractData_ShouldReturnCleanJson() throws Exception {
        String expectedJson = "{\"title\": \"Flug Berlin\", \"startDate\": \"2026-06-01T10:00:00Z\", " +
                "\"endDate\": \"2026-06-02T18:00:00Z\", \"location\": \"Berlin\"}";
        Map<String, Object> mockResponse = Map.of(
                "choices", List.of(Map.of("message", Map.of("content", expectedJson))));
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(mockResponse);

        byte[] pdfBytes = createTestPdf("Flug Berlin, 01.06.2026, Berlin Hauptbahnhof");
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new ByteArrayInputStream(pdfBytes));

        String result = documentScannerService.scanAndExtractData(mockFile);
        assertEquals(expectedJson, result);
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    public void testScanAndExtractData_NullResponse_ShouldThrow() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);
        byte[] pdfBytes = createTestPdf("Test");
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new ByteArrayInputStream(pdfBytes));
        assertThrows(NullPointerException.class,
                () -> documentScannerService.scanAndExtractData(mockFile));
    }

    @Test
    public void testScanAndExtractData_EmptyChoices_ShouldThrow() throws Exception {
        Map<String, Object> mockResponse = Map.of("choices", Collections.emptyList());
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(mockResponse);
        byte[] pdfBytes = createTestPdf("Test");
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new ByteArrayInputStream(pdfBytes));
        assertThrows(IndexOutOfBoundsException.class,
                () -> documentScannerService.scanAndExtractData(mockFile));
    }

    @Test
    public void testScanAndExtractData_InvalidPdf_ShouldThrow() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Das ist kein PDF".getBytes());
        assertThrows(java.io.IOException.class,
                () -> documentScannerService.scanAndExtractData(invalidFile));
    }

    @Test
    public void testScanAndExtractData_GroqApiError_ShouldThrow() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(HttpClientErrorException.class);
        byte[] pdfBytes = createTestPdf("Test");
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new ByteArrayInputStream(pdfBytes));
        assertThrows(HttpClientErrorException.class,
                () -> documentScannerService.scanAndExtractData(mockFile));
    }
}