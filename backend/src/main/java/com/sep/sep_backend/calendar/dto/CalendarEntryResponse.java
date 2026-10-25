package com.sep.sep_backend.calendar.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents the response body for a calendar entry.
 * Contains only the fields relevant for the frontend,
 * including an optional reference to a linked travel entry.
 */
public class CalendarEntryResponse {

    private UUID id;
    private String title;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private String location;
    private String description;
    private String color;

    /**
     * The ID of the linked travel entry, if this calendar entry
     * was automatically created from a trip. Null for manual entries.
     */
    private UUID travelEntryId;

    public CalendarEntryResponse() {}

    public CalendarEntryResponse(UUID id, String title, ZonedDateTime startDate,
                                 ZonedDateTime endDate, String location,
                                 String description, String color, UUID travelEntryId) {
        this.id = id;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.description = description;
        this.color = color;
        this.travelEntryId = travelEntryId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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