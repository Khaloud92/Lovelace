package com.sep.sep_backend.auth;

import com.sep.sep_backend.auth.dto.LoginRequest;
import com.sep.sep_backend.auth.dto.LoginResponse;
import com.sep.sep_backend.registration.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Verifies user credentials and issues JWTs.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Checks email, password, and verification status, then issues a JWT.
     *
     * @param request email and password
     * @return the token and basic user data
     */
    public LoginResponse login(LoginRequest request) {
        var user = appUserRepository
                .findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before logging in.");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new LoginResponse(token, user.getName(), user.getEmail(), user.getId().toString());
    }
}
