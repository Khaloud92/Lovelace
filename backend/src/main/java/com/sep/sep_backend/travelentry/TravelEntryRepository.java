package com.sep.sep_backend.travelentry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link TravelEntry} entities.
 */
public interface TravelEntryRepository extends JpaRepository<TravelEntry, UUID> {
    /** Returns all travel entries of the given user. */
    List<TravelEntry> findByUserId(String userId);
}