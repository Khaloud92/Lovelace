package com.sep.sep_backend.chat.dto;

/**
 * DTO for sending a new text message to an existing conversation.
 */
public record SendMessageRequest(
        String content
) {}