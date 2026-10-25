package com.sep.sep_backend.emailcost.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for creating an email cost from pasted email text.
 */
@Getter
@Setter
public class EmailCostImportRequest {

    private String userId;

    private UUID travelEntryId;

    private String subject;

    private String emailText;

    private String source;
}