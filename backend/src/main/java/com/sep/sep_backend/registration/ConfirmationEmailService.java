package com.sep.sep_backend.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the email-verification mail after registration.
 */
@Service
@RequiredArgsConstructor
public class ConfirmationEmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends the verification link to the user's registered address.
     *
     * @param user the newly registered user
     */
    public void sendRegistrationConfirmation(AppUser user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your email");
        message.setText("Hi " + user.getName() + ",\n\nPlease verify your email by clicking the link below:\n\n"
                + "http://localhost:4200/#/verify-email?token=" + user.getConfirmationToken()
                + "\n\nIf you did not register, ignore this email.");
        mailSender.send(message);
    }
}