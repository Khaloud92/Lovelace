package com.sep.sep_backend.registration;

import com.sep.sep_backend.registration.dto.RegistrationRequest;
import com.sep.sep_backend.registration.dto.RegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Business logic for account registration and email verification.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AppUserRepository appUserRepository;

    private final PasswordEncoder passwordEncoder;

    private final ConfirmationEmailService confirmationEmailService;

    /**
     * Creates an unverified account and sends the confirmation mail.
     *
     * @param request name, email, and password
     * @return the created account data
     */
    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String token = UUID.randomUUID().toString();

        AppUser user = new AppUser();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setConfirmationToken(token);
        user.setEmailVerified(false);

        AppUser savedUser = appUserRepository.save(user);

        confirmationEmailService.sendRegistrationConfirmation(savedUser);

        return new RegistrationResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                "Registration successful. Please check your email to verify your account."
        );
    }

    /**
     * Marks the account matching the token as verified and invalidates the token.
     *
     * @param token the confirmation token
     */
    public void confirmEmail(String token) {
        AppUser user = appUserRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired confirmation token"));

        user.setEmailVerified(true);
        user.setConfirmationToken(null);
        appUserRepository.save(user);
    }
}