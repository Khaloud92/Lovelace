package com.sep.sep_backend.search;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link SearchQuery} entities.
 */
public interface SearchQueryRepository extends JpaRepository<SearchQuery, UUID> {

    Optional<SearchQuery> findByUserIdAndQueryTextIgnoreCase(String userId, String queryText);

    List<SearchQuery> findTop5ByUserIdOrderBySearchCountDescLastSearchedAtDesc(String userId);

    void deleteByUserIdAndQueryTextIgnoreCase(String userId, String queryText);
}