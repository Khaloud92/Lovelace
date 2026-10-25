package com.sep.sep_backend.emailcost.mailbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link MailboxConnection} entities.
 */
public interface MailboxConnectionRepository extends JpaRepository<MailboxConnection, UUID> {

    Optional<MailboxConnection> findFirstByUserIdOrderByConnectedAtDesc(String userId);

    Optional<MailboxConnection> findFirstByUserIdAndActiveTrueOrderByConnectedAtDesc(String userId);
}