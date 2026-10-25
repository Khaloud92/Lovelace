package com.sep.sep_backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing {@link GroupTrip} entities.
 */
@Repository
public interface GroupTripRepository extends JpaRepository<GroupTrip, UUID> {
}
