package com.sep.sep_backend.Filemanager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link Document} entities.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Returns all documents belonging to the given travel entry.
     *
     * @param travelEntryId id of the travel entry
     * @return the documents of that trip
     */
    List<Document> findByTravelEntryId(UUID travelEntryId);
}
