package com.sep.sep_backend.chat.dto;

import com.sep.sep_backend.chat.GroupTrip;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a group trip proposal, including every member's response.
 */
public record GroupTripDto(
        UUID id,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        UUID proposedById,
        String proposedByName,
        List<TripParticipantDto> participants
) {

    public static GroupTripDto from(GroupTrip trip) {
        List<TripParticipantDto> participants = trip.getParticipants().stream()
                .map(p -> new TripParticipantDto(
                        p.getUser().getId(),
                        p.getUser().getName(),
                        p.getStatus().name()))
                .toList();

        return new GroupTripDto(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getDescription(),
                trip.getProposedBy().getId(),
                trip.getProposedBy().getName(),
                participants
        );
    }
}
