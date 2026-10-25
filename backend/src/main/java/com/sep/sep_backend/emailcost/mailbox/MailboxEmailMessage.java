package com.sep.sep_backend.emailcost.mailbox;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A single email fetched from the mailbox, reduced to the fields needed for cost parsing.
 */
@Getter
@AllArgsConstructor
public class MailboxEmailMessage {

    private String externalMessageId;

    private String subject;

    private String text;
}