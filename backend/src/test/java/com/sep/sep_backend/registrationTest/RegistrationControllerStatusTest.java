package com.sep.sep_backend.registrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.sep_backend.registration.RegistrationController;
import com.sep.sep_backend.registration.RegistrationService;
import com.sep.sep_backend.registration.dto.RegistrationRequest;
import com.sep.sep_backend.registration.dto.RegistrationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationControllerStatusTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        RegistrationController registrationController = new RegistrationController(registrationService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(registrationController)
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void register_shouldReturnCreatedWhenRegistrationIsSuccessful() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Allan");
        request.setEmail("allan@example.com");
        request.setPassword("password123");

        RegistrationResponse response = new RegistrationResponse(
                UUID.randomUUID(),
                "Allan",
                "allan@example.com",
                "Registration successful. Confirmation email sent."
        );

        when(registrationService.register(any(RegistrationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Allan"))
                .andExpect(jsonPath("$.email").value("allan@example.com"))
                .andExpect(jsonPath("$.message").value("Registration successful. Confirmation email sent."));

        verify(registrationService).register(any(RegistrationRequest.class));
    }

    @Test
    void register_shouldReturnBadRequestWhenEmailFormatIsInvalid() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Allan");
        request.setEmail("invalid-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email format is invalid"));

        verify(registrationService, never()).register(any(RegistrationRequest.class));
    }

    @Test
    void register_shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Allan");
        request.setEmail("allan-short-password@example.com");
        request.setPassword("123");

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long"));

        verify(registrationService, never()).register(any(RegistrationRequest.class));
    }

    @Test
    void register_shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Allan");
        request.setEmail("allan-empty-password@example.com");
        request.setPassword("  ");

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required"));

        verify(registrationService, never()).register(any(RegistrationRequest.class));
    }

    @Test
    void register_shouldReturnBadRequestWhenNameIsEmpty() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("");
        request.setEmail("empty-name@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name is required"));

        verify(registrationService, never()).register(any(RegistrationRequest.class));
    }

    @Test
    void register_shouldReturnBadRequestWhenEmailIsAlreadyRegistered() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Sara");
        request.setEmail("sara@example.com");
        request.setPassword("password123");

        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));

        verify(registrationService).register(any(RegistrationRequest.class));
    }
}