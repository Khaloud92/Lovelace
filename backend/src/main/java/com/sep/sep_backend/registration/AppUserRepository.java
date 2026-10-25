package com.sep.sep_backend.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link AppUser} entities.
 */
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    boolean existsByEmail(String email);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByConfirmationToken(String token);

    /**
     * Case-insensitive substring search over name and email, excluding one user
     * (typically the searcher themselves).
     *
     * @param query     the search text
     * @param excludeId user id to leave out of the results
     * @return matching users
     */
    @Query("""
        SELECT u FROM AppUser u
        WHERE u.id <> :excludeId
          AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
    List<AppUser> searchByNameOrEmail(@Param("query") String query,
                                      @Param("excludeId") UUID excludeId);
}