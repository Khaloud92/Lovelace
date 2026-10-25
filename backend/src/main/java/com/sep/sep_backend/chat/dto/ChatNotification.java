package com.sep.sep_backend.chat.dto;

import java.util.UUID;

/**
 * Lightweight event pushed to every conversation member (except the sender)
 * when a new message arrives, so clients can show an unread indicator without
 * subscribing to every conversation topic individually.
 */
public record ChatNotification(
        UUID conversationId,
        UUID senderId
) {}
