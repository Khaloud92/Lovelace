package com.sep.sep_backend.emailcost.mailbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user's IMAP mailbox connection. The app password is stored encrypted.
 */
@Entity
@Getter
@Setter
public class MailboxConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String userId;

    private String emailAddress;

    private String provider;

    private String imapHost;

    private int imapPort;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String appPassword;

    private boolean active;

    private LocalDateTime connectedAt;
}