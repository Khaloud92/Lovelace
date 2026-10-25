package com.sep.sep_backend.SighseeingSugesstion.dto;

import java.util.List;

/**
 * Response DTO containing a list of AI-generated sightseeing suggestions.
 *
 * @param sights List of recommended sightseeing locations.
 */
public record AiSightseeingListResponse(
        List<AiSightseeingSuggestionRequest> sights
) {}