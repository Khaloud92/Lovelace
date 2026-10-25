package com.sep.sep_backend.emailcost;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link EmailCost} entities.
 */
public interface EmailCostRepository extends JpaRepository<EmailCost, UUID> {

    List<EmailCost> findByUserIdAndIgnoredFalseOrderByImportedAtDesc(String userId);

    List<EmailCost> findByTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(UUID travelEntryId);

    List<EmailCost> findByUserIdAndTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(
            String userId,
            UUID travelEntryId
    );

    boolean existsByUserIdAndExternalMessageIdAndIgnoredFalse(String userId, String externalMessageId);

    Optional<EmailCost> findFirstByUserIdAndExternalMessageIdOrderByImportedAtDesc(
            String userId,
            String externalMessageId
    );

    Optional<EmailCost> findByIdAndUserId(UUID id, String userId);
}