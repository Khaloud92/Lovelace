package com.sep.sep_backend.chat.dto;

import java.util.UUID;

/**
 * DTO for initiating or opening a direct 1:1 chat with a friend.
 */
public record CreateDirectRequest(
        UUID friendId
) {}