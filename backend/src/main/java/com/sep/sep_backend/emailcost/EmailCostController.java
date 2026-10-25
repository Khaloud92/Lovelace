package com.sep.sep_backend.emailcost;

import com.sep.sep_backend.emailcost.dto.EmailCostResponse;
import com.sep.sep_backend.emailcost.dto.FetchEmailCostsRequest;
import com.sep.sep_backend.emailcost.dto.MailboxConnectionRequest;
import com.sep.sep_backend.emailcost.dto.MailboxStatusResponse;
import com.sep.sep_backend.emailcost.mailbox.MailboxConnection;
import com.sep.sep_backend.emailcost.mailbox.MailboxConnectionRepository;
import com.sep.sep_backend.emailcost.mailbox.MailboxReaderService;
import com.sep.sep_backend.registration.AppUser;
import com.sep.sep_backend.registration.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for importing costs from emails and managing the mailbox connection.
 */
@RestController
@RequestMapping("/api/email-costs")
public class EmailCostController {

    private final EmailCostService emailCostService;
    private final MailboxConnectionRepository mailboxConnectionRepository;
    private final MailboxReaderService mailboxReaderService;
    private final AppUserRepository appUserRepository;


    public EmailCostController(
            EmailCostService emailCostService,
            MailboxConnectionRepository mailboxConnectionRepository,
            MailboxReaderService mailboxReaderService,
            AppUserRepository appUserRepository
    ) {
        this.emailCostService = emailCostService;
        this.mailboxConnectionRepository = mailboxConnectionRepository;
        this.mailboxReaderService = mailboxReaderService;
        this.appUserRepository = appUserRepository;
    }



    /**
     * Returns the authenticated user's email costs, optionally filtered by trip.
     *
     * @param travelEntryId optional trip filter
     * @return the matching email costs
     */
    @GetMapping
    public ResponseEntity<List<EmailCostResponse>> getEmailCosts(
            Authentication authentication,
            @RequestParam(required = false) UUID travelEntryId
    ) {
        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(emailCostService.getEmailCosts(userId, travelEntryId));
    }

    /**
     * Imports new costs from the connected mailbox.
     *
     * @param request optional trip selection
     * @return the email costs after the import
     */
    @PostMapping("/fetch")
    public ResponseEntity<List<EmailCostResponse>> fetchCosts(
            Authentication authentication,
            @RequestBody(required = false) FetchEmailCostsRequest request
    ) {
        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(emailCostService.fetchCostsFromConnectedMailbox(userId, request));
    }

    /**
     * Detaches an email cost from its trip.
     *
     * @param emailCostId id of the email cost
     * @return 204 on success
     */
    @DeleteMapping("/{emailCostId}")
    public ResponseEntity<Void> removeEmailCostFromTrip(
            Authentication authentication,
            @PathVariable UUID emailCostId
    ) {
        String userId = getCurrentUserId(authentication);
        emailCostService.removeEmailCostFromTrip(userId, emailCostId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently ignores an email cost for future imports.
     *
     * @param emailCostId id of the email cost
     * @return 204 on success
     */
    @PutMapping("/{emailCostId}/ignore")
    public ResponseEntity<Void> ignoreEmailCostForever(
            Authentication authentication,
            @PathVariable UUID emailCostId
    ) {
        String userId = getCurrentUserId(authentication);
        emailCostService.ignoreEmailCostForever(userId, emailCostId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Connects the user's mailbox via IMAP and verifies the credentials.
     *
     * @param request email address, provider, and app password
     * @return the resulting mailbox status
     */
    @PostMapping("/mailbox/connect")
    public ResponseEntity<MailboxStatusResponse> connectMailbox(
            Authentication authentication,
            @RequestBody MailboxConnectionRequest request
    ) {
        String userId = getCurrentUserId(authentication);

        if (request == null) {
            throw new IllegalArgumentException("Mailbox connection request is required");
        }

        if (request.getEmailAddress() == null || request.getEmailAddress().isBlank()) {
            throw new IllegalArgumentException("Email address is required");
        }

        if (request.getAppPassword() == null || request.getAppPassword().isBlank()) {
            throw new IllegalArgumentException("App password is required");
        }

        MailboxConnection connection = mailboxConnectionRepository
                .findFirstByUserIdOrderByConnectedAtDesc(userId)
                .orElseGet(MailboxConnection::new);

        connection.setUserId(userId);
        connection.setEmailAddress(request.getEmailAddress());
        connection.setProvider(
                request.getProvider() == null || request.getProvider().isBlank()
                        ? "GMAIL"
                        : request.getProvider()
        );
        connection.setImapHost("imap.gmail.com");
        connection.setImapPort(993);
        connection.setAppPassword(request.getAppPassword().replaceAll("\\s", ""));
        connection.setActive(true);

        mailboxReaderService.testConnection(connection);

        MailboxConnection savedConnection = mailboxConnectionRepository.save(connection);

        return ResponseEntity.ok(toMailboxStatusResponse(userId, savedConnection));
    }

    /**
     * Returns whether the user currently has an active mailbox connection.
     *
     * @return the mailbox status
     */
    @GetMapping("/mailbox/status")
    public ResponseEntity<MailboxStatusResponse> getMailboxStatus(Authentication authentication) {
        String userId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                mailboxConnectionRepository
                        .findFirstByUserIdAndActiveTrueOrderByConnectedAtDesc(userId)
                        .map(connection -> toMailboxStatusResponse(userId, connection))
                        .orElseGet(() -> disconnectedStatus(userId))
        );
    }

    /**
     * Deactivates the mailbox connection and discards the stored app password.
     *
     * @return the disconnected mailbox status
     */
    @DeleteMapping("/mailbox/disconnect")
    public ResponseEntity<MailboxStatusResponse> disconnectMailbox(Authentication authentication) {
        String userId = getCurrentUserId(authentication);

        mailboxConnectionRepository
                .findFirstByUserIdAndActiveTrueOrderByConnectedAtDesc(userId)
                .ifPresent(connection -> {
                    connection.setActive(false);
                    connection.setAppPassword(null);
                    mailboxConnectionRepository.save(connection);
                });

        return ResponseEntity.ok(disconnectedStatus(userId));
    }

    private MailboxStatusResponse toMailboxStatusResponse(String userId, MailboxConnection connection) {
        return new MailboxStatusResponse(
                userId,
                connection.isActive(),
                connection.getEmailAddress(),
                connection.getProvider(),
                connection.getConnectedAt()
        );
    }

    private MailboxStatusResponse disconnectedStatus(String userId) {
        return new MailboxStatusResponse(
                userId,
                false,
                null,
                "GMAIL",
                null
        );
    }

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user");
        }

        String email = authentication.getName();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}