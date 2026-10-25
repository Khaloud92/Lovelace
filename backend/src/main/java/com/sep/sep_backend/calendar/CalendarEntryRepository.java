package com.sep.sep_backend.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing CalendarEntry entities.
 */
public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, UUID> {

    /** Returns all calendar entries of the given user. */
    List<CalendarEntry> findByUserId(String userId);
}