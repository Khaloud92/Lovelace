package com.sep.sep_backend.registrationTest;

import com.sep.sep_backend.registration.RegistrationController;
import com.sep.sep_backend.registration.RegistrationService;
import com.sep.sep_backend.registration.dto.RegistrationRequest;
import com.sep.sep_backend.registration.dto.RegistrationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationControllerTest {

    private RegistrationService registrationService;

    private RegistrationController registrationController;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        registrationController = new RegistrationController(registrationService);
    }

    @Test
    void register_shouldReturnCreatedWhenRegistrationIsSuccessful() {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Allan");
        request.setEmail("allan@example.com");
        request.setPassword("password123");

        RegistrationResponse serviceResponse = new RegistrationResponse(
                UUID.randomUUID(),
                "Allan",
                "allan@example.com",
                "Registration successful. Confirmation email sent."
        );

        when(registrationService.register(request))
                .thenReturn(serviceResponse);

        ResponseEntity<RegistrationResponse> response =
                registrationController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        assertThat(response.getBody().getName()).isEqualTo("Allan");
        assertThat(response.getBody().getEmail()).isEqualTo("allan@example.com");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Registration successful. Confirmation email sent.");

        verify(registrationService).register(request);
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequestWithMessage() {
        IllegalArgumentException exception =
                new IllegalArgumentException("Email is already registered");

        ResponseEntity<Map<String, String>> response =
                registrationController.handleIllegalArgumentException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry(
                "message",
                "Email is already registered"
        );
    }
}