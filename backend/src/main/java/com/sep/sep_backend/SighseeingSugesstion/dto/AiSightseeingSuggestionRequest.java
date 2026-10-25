package com.sep.sep_backend.SighseeingSugesstion.dto;

/**
 * Request DTO representing a single AI-suggested sightseeing location.
 *
 * @param name   The name or title of the sightseeing spot.
 * @param amount The estimated cost, rating, or score associated with the suggestion.
 */
public record AiSightseeingSuggestionRequest(
        String name,
        Double amount
) {}