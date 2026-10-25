package com.sep.sep_backend.friendship.dto;

import java.util.UUID;

/**
 * DTO for sending a friend request to another user.
 */
public record sendFriendRequestDto(
        /** The unique identifier of the user receiving the request. */
        UUID addresseeID
) {}