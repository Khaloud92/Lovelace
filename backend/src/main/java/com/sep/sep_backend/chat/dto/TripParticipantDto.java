package com.sep.sep_backend.chat.dto;

import java.util.UUID;

/**
 * DTO representing a single member's response state for a group trip.
 */
public record TripParticipantDto(
        UUID userId,
        String userName,
        String status
) {}
