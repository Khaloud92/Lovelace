package com.sep.sep_backend.friendship.dto;

import com.sep.sep_backend.friendship.Friendship;

import java.util.UUID;

/**
 * DTO for responding to an incoming friend request.
 */
public record RespondRequestDto(
        /** The ID of the friendship request being answered. */
        UUID friendshipId,
        /** The decision made (e.g., ACCEPTED or DECLINED). */
        Friendship.Status decision
) {}