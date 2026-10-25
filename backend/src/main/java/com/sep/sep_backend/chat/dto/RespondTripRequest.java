package com.sep.sep_backend.chat.dto;

import com.sep.sep_backend.chat.GroupTripParticipant;

/**
 * Request payload for accepting or declining a group trip proposal.
 */
public record RespondTripRequest(
        GroupTripParticipant.Status decision
) {}
