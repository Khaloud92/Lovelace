package com.sep.sep_backend.profile.dto;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Live location update pushed to friends over WebSocket; sharing=false tells
 * clients to remove the pin.
 */
public record FriendLocationEvent(
        UUID userId,
        String name,
        String profilePicture,
        Double latitude,
        Double longitude,
        LocalDateTime updatedAt,
        boolean sharing
) {}
