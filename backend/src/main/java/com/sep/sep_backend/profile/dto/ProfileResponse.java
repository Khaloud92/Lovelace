package com.sep.sep_backend.profile.dto;


/**
 * Response body with the user's profile data.
 */
public record ProfileResponse(
        String name,
        String email,
        String profilePicture,
        boolean shareLocation
) {}
