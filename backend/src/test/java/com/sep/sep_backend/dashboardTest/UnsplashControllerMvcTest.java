package com.sep.sep_backend.dashboard;

import com.sep.sep_backend.auth.JwtService;
import com.sep.sep_backend.config.SecurityConfig;
import com.sep.sep_backend.registration.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UnsplashController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
class UnsplashControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void shouldRedirectToImageUrl() throws Exception {
        Map<String, Object> mockResponse = Map.of("urls",
                Map.of("regular", "https://images.unsplash.com/photo-tokyo.jpg"));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/image").param("destination", "Tokyo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://images.unsplash.com/photo-tokyo.jpg"));
    }

    @Test
    void shouldReturnBadRequestWhenDestinationIsMissing() throws Exception {
        mockMvc.perform(get("/api/image"))
                .andExpect(status().isBadRequest());
    }
}