package com.sep.sep_backend.chat.dto;

import java.util.UUID;

/**
 * Response for proposing or accepting a group trip. Carries the chat message
 * plus the acting user's own travel entry id (null when declining), so the
 * frontend can offer creating a personal budget for it.
 */
public record TripActionResponse(
        MessageDto message,
        UUID travelEntryId
) {}
