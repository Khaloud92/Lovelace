package com.sep.sep_backend.friendship;

import com.sep.sep_backend.registration.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Friendship} entities.
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /**
     * Checks if a directional friendship request exists.
     */
    boolean existsByRequesterAndAddressee(AppUser requester, AppUser addressee);

    /**
     * Finds a friendship by specific requester and addressee.
     */
    Optional<Friendship> findByRequesterAndAddressee(AppUser requester, AppUser addressee);

    /**
     * Finds a friendship between two users in either direction.
     */
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester = :a AND f.addressee = :b)
           OR (f.requester = :b AND f.addressee = :a)
        """)
    Optional<Friendship> findBetween(@Param("a") AppUser a, @Param("b") AppUser b);

    /**
     * Finds incoming requests for a user by status.
     */
    List<Friendship> findByAddresseeAndStatus(AppUser addressee, Friendship.Status status);

    /**
     * Finds outgoing requests from a user by status.
     */
    List<Friendship> findByRequesterAndStatus(AppUser requester, Friendship.Status status);

    /**
     * Finds all friendships for a user matching a specific status.
     */
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester = :user OR f.addressee = :user)
          AND f.status = :status
        """)
    List<Friendship> findAllForUserByStatus(@Param("user") AppUser user,
                                            @Param("status") Friendship.Status status);
}