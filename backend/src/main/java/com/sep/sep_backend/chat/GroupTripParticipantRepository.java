package com.sep.sep_backend.chat;

import com.sep.sep_backend.registration.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link GroupTripParticipant} relations.
 */
@Repository
public interface GroupTripParticipantRepository extends JpaRepository<GroupTripParticipant, UUID> {

    /** Returns the given user's response entry for a trip, if they participate. */
    Optional<GroupTripParticipant> findByTripAndUser(GroupTrip trip, AppUser user);
}
