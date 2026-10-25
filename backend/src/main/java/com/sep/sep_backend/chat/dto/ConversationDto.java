package com.sep.sep_backend.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a conversation preview, including participant details and the latest message.
 */
public record ConversationDto(
        UUID id,
        String type,
        String displayName,
        String displayAvatar,
        List<MemberDto> members,
        String lastMessage,
        LocalDateTime lastMessageAt
) {}