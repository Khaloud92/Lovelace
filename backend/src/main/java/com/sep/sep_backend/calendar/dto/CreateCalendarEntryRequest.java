package com.sep.sep_backend.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents the request body for creating a new calendar entry.
 * <p>
 * A title and a start date are required.
 * End date, location, description, and travelEntryId are optional.
 */
public class CreateCalendarEntryRequest {

    @NotBlank
    private String title;

    @NotNull
    private ZonedDateTime startDate;

    private ZonedDateTime endDate;
    private String location;
    private String description;

    @NotNull
    private String color;

    private UUID travelEntryId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ZonedDateTime getStartDate() { return startDate; }
    public void setStartDate(ZonedDateTime startDate) { this.startDate = startDate; }

    public ZonedDateTime getEndDate() { return endDate; }
    public void setEndDate(ZonedDateTime endDate) { this.endDate = endDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public UUID getTravelEntryId() { return travelEntryId; }
    public void setTravelEntryId(UUID travelEntryId) { this.travelEntryId = travelEntryId; }
}