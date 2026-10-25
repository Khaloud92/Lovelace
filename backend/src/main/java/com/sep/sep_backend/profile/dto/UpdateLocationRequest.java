package com.sep.sep_backend.profile.dto;


/**
 * Request body for updating the user's current position.
 */
public record UpdateLocationRequest(double latitude, double longitude) {}
