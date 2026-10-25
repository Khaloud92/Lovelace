package com.sep.sep_backend.auth.dto;

/**
 * Response body after a successful login.
 *
 * @param token  the issued JWT
 * @param name   display name of the user
 * @param email  email of the user
 * @param userId id of the user
 */
public record LoginResponse(String token, String name, String email, String userId) {
}
