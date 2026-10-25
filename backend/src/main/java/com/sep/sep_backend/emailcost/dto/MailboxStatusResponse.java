package com.sep.sep_backend.emailcost.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response body describing whether a mailbox is currently connected.
 */
@Getter
@AllArgsConstructor
public class MailboxStatusResponse {

    private String userId;

    private boolean connected;

    private String emailAddress;

    private String provider;

    private LocalDateTime connectedAt;
}