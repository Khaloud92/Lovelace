package com.sep.sep_backend.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Response body after a successful registration.
 */
@Getter
@AllArgsConstructor
public class RegistrationResponse {

    private UUID id;

    private String name;

    private String email;

    private String message;
}