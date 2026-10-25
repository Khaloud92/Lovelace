package com.sep.sep_backend.friendship.dto;

import com.sep.sep_backend.friendship.Friendship;

import java.util.UUID;

/**
 * DTO representing a friendship relation from the perspective of the current user.
 */
public record FriendshipResponseDto(
        /** Unique identifier of the friendship relation. */
        UUID friendshipId,
        /** Unique identifier of the other user in the relationship. */
        UUID otherUserId,
        /** Name of the other user. */
        String otherUserName,
        /** Profile picture of the other user as a data URL, or null if none is set. */
        String otherUserProfilePicture,
        /** Current status of the friendship. */
        Friendship.Status status
) {}