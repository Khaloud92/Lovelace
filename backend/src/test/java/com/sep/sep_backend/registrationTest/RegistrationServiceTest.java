package com.sep.sep_backend.registrationTest;

import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.registration.ConfirmationEmailService;
import com.sep.sep_backend.registration.RegistrationService;
import com.sep.sep_backend.registration.dto.RegistrationRequest;
import com.sep.sep_backend.registration.dto.RegistrationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ConfirmationEmailService confirmationEmailService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void register_shouldSaveUserWithEncryptedPasswordAndSendConfirmationEmail() {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("  Allan  ");
        request.setEmail("  ALLAN@EXAMPLE.COM  ");
        request.setPassword("password123");

        when(appUserRepository.existsByEmail("allan@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$encrypted-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> {
                    AppUser user = invocation.getArgument(0);
                    user.setId(UUID.randomUUID());
                    return user;
                });

        RegistrationResponse response = registrationService.register(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Allan");
        assertThat(response.getEmail()).isEqualTo("allan@example.com");
        assertThat(response.getMessage()).isEqualTo("Registration successful. Please check your email to verify your account.");

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());

        AppUser savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("Allan");
        assertThat(savedUser.getEmail()).isEqualTo("allan@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$encrypted-password");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("password123");

        verify(passwordEncoder).encode("password123");
        verify(confirmationEmailService).sendRegistrationConfirmation(savedUser);
    }

    @Test
    void register_shouldThrowExceptionWhenEmailIsAlreadyRegistered() {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Sara");
        request.setEmail("sara@example.com");
        request.setPassword("password123");

        when(appUserRepository.existsByEmail("sara@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already registered");

        verify(appUserRepository, never()).save(any(AppUser.class));
        verify(passwordEncoder, never()).encode(any());
        verify(confirmationEmailService, never()).sendRegistrationConfirmation(any(AppUser.class));
    }
}