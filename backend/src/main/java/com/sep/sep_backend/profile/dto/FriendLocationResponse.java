package com.sep.sep_backend.profile.dto;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * A friend's shared position.
 */
public record FriendLocationResponse(
        UUID userId,
        String name,
        String profilePicture,
        double latitude,
        double longitude,
        LocalDateTime updatedAt
) {}
