package com.sep.sep_backend.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

/**
 * Represents the request body for updating an existing calendar entry.
 * <p>
 * A title, a start date, and a color are required.
 * End date, location, and description are optional.
 */
public class UpdateCalendarEntryRequest {

    @NotBlank
    private String title;

    @NotNull
    private ZonedDateTime startDate;

    private ZonedDateTime endDate;
    private String location;
    private String description;

    @NotNull
    private String color;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }

    public ZonedDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
