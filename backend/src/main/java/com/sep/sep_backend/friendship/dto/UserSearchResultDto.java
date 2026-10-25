package com.sep.sep_backend.friendship.dto;

import java.util.UUID;

/**
 * DTO representing a user search result along with the current friendship relationship.
 */
public record UserSearchResultDto(
        /** The unique identifier of the found user. */
        UUID userId,
        /** The name of the user. */
        String name,
        /** The email address of the user. */
        String email,
        /** The user's profile picture as a data URL, or null if none is set. */
        String profilePicture,
        /** The current friendship state between the searching user and this user. */
        Relationship relationship
) {
    /**
     * Enum defining the possible relationship states during a user search.
     */
    public enum Relationship {
        /** No friendship relation or pending request exists. */
        NONE,
        /** A friend request has been sent to this user. */
        REQUEST_SENT,
        /** A friend request has been received from this user. */
        REQUEST_RECEIVED,
        /** The users are already accepted friends. */
        FRIENDS
    }
}