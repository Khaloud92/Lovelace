package com.sep.sep_backend.chat.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new group chat with a specified name and initial members.
 */
public record CreateGroupRequest(
        String name,
        List<UUID> memberIds
) {}