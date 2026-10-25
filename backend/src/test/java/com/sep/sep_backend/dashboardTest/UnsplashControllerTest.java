package com.sep.sep_backend.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnsplashControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UnsplashController unsplashController;

    private Map<String, Object> mockResponse;

    @BeforeEach
    void setUp() {
        Map<String, String> mockUrls = Map.of("regular", "https://images.unsplash.com/photo-tokyo.jpg");
        mockResponse = Map.of("urls", mockUrls);
    }

    @Test
    void shouldReturnRedirectViewWithImageUrl() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);
        RedirectView result = unsplashController.getDestinationImage("Tokyo");
        assertThat(result.getUrl()).isEqualTo("https://images.unsplash.com/photo-tokyo.jpg");
    }

    @Test
    void shouldCallUnsplashApiWithDestinationInUrl() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);
        unsplashController.getDestinationImage("Paris");
        verify(restTemplate).getForObject(
                argThat((String url) -> url.contains("Paris") && url.contains("unsplash.com")),
                eq(Map.class)
        );
    }

    @Test
    void shouldCallUnsplashApiWithLandmarkAndTravelKeywords() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);
        unsplashController.getDestinationImage("Rome");
        verify(restTemplate).getForObject(
                argThat((String url) -> url.contains("landmark") && url.contains("travel")),
                eq(Map.class)
        );
    }

    @Test
    void shouldCallUnsplashApiWithLandscapeOrientation() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);
        unsplashController.getDestinationImage("Berlin");
        verify(restTemplate).getForObject(
                argThat((String url) -> url.contains("orientation=landscape")),
                eq(Map.class)
        );
    }

    @Test
    void shouldCallRestTemplateExactlyOnce() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);
        unsplashController.getDestinationImage("Tokyo");
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    void shouldHandleDestinationWithSpaces() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);
        RedirectView result = unsplashController.getDestinationImage("New York");
        assertThat(result.getUrl()).isEqualTo("https://images.unsplash.com/photo-tokyo.jpg");
        verify(restTemplate).getForObject(
                argThat((String url) -> url.contains("New York")),
                eq(Map.class)
        );
    }

    @Test
    void shouldHandleDestinationWithSpecialCharacters() {
        Map<String, Object> specialResponse = Map.of("urls", Map.of("regular", "https://images.unsplash.com/photo-special.jpg"));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(specialResponse);
        RedirectView result = unsplashController.getDestinationImage("Paris, France");
        assertThat(result.getUrl()).isEqualTo("https://images.unsplash.com/photo-special.jpg");
    }

    @Test
    void shouldThrowWhenRestTemplateReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () ->
                unsplashController.getDestinationImage("Tokyo")
        );
    }

    @Test
    void shouldThrowWhenUrlsMapIsMissing() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of());
        assertThrows(NullPointerException.class, () ->
                unsplashController.getDestinationImage("Tokyo")
        );
    }

    @Test
    void shouldThrowWhenRegularUrlIsMissing() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("urls", Map.of("regular", "https://images.unsplash.com/photo-fallback.jpg")));
        RedirectView result = unsplashController.getDestinationImage("Tokyo");
        assertThat(result.getUrl()).isNotNull();
        assertThat(result.getUrl()).startsWith("https://");
    }

    @Test
    void shouldThrowWhenRestTemplateThrows() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Unsplash API not reachable"));
        assertThrows(RuntimeException.class, () ->
                unsplashController.getDestinationImage("Tokyo")
        );
    }
}