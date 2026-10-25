package com.sep.sep_backend.emailcost.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for connecting a mailbox.
 */
@Getter
@Setter
public class MailboxConnectionRequest {
    private String emailAddress;
    private String provider;
    private String appPassword;
}