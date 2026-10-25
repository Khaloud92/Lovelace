package com.sep.sep_backend.chat.dto;

import java.time.LocalDate;

/**
 * Request payload for proposing a group trip inside a group chat.
 */
public record ProposeTripRequest(
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String description
) {}
