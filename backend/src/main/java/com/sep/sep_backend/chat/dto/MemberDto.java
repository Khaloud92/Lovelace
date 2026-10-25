package com.sep.sep_backend.chat.dto;

import java.util.UUID;

/**
 * DTO representing a simplified participant within a chat conversation.
 */
public record MemberDto(
        UUID userId,
        String name,
        String profilePicture
) {}