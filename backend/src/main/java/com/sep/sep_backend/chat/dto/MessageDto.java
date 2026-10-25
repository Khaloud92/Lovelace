package com.sep.sep_backend.chat.dto;

import com.sep.sep_backend.chat.Message;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a single chat message with its content and sender details.
 * Carries the group trip proposal when the message announces one.
 */
public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        LocalDateTime sentAt,
        GroupTripDto trip
) {

    public static MessageDto from(Message m) {
        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getContent(),
                m.getSentAt(),
                m.getGroupTrip() != null ? GroupTripDto.from(m.getGroupTrip()) : null
        );
    }
}
