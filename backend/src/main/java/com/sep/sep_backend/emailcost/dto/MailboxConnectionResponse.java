package com.sep.sep_backend.emailcost.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body describing a stored mailbox connection.
 */
@Getter
@AllArgsConstructor
public class MailboxConnectionResponse {

    private UUID id;

    private String userId;

    private String emailAddress;

    private String provider;

    private boolean connected;

    private LocalDateTime connectedAt;

    private String message;
}