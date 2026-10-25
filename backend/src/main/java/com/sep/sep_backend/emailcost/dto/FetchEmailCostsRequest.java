package com.sep.sep_backend.emailcost.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for a mailbox import run, selecting the target trip.
 */
@Getter
@Setter
public class FetchEmailCostsRequest {

    private UUID travelEntryId;
}