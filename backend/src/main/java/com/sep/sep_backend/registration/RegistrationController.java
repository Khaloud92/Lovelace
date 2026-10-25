package com.sep.sep_backend.registration;

import com.sep.sep_backend.registration.dto.RegistrationRequest;
import com.sep.sep_backend.registration.dto.RegistrationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for account registration and email verification.
 */
@RestController
@RequestMapping("/api/registration")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Verifies an email address via the token from the confirmation mail.
     *
     * @param token the confirmation token
     * @return a success message, or 400 for an invalid token
     */
    @GetMapping("/confirm")
    public ResponseEntity<?> confirmEmail(@RequestParam String token) {
        try {
            registrationService.confirmEmail(token);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Registers a new account and triggers the verification mail.
     *
     * @param request name, email, and password
     * @return the created account data
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request
    ) {
        RegistrationResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Maps business errors (e.g. duplicate email) to a 400 response. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    /** Returns the most relevant validation error as a 400 response, preferring missing-field errors. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .filter(this::isRequiredFieldError)
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElseGet(() -> exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(FieldError::getDefaultMessage)
                        .orElse("Validation failed"));

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", message));
    }

    private boolean isRequiredFieldError(FieldError error) {
        return "NotBlank".equals(error.getCode());
    }
}