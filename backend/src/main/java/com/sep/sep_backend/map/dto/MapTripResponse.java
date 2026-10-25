package com.sep.sep_backend.map.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response body for a trip shown on the map, including owner info and coordinates.
 */
public class MapTripResponse {

    private UUID id;
    private String title;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String color;
    private double latitude;
    private double longitude;
    private String ownerId;
    private String ownerName;
    private String ownerProfilePicture;
    private boolean own;

    public MapTripResponse() {}

    public MapTripResponse(UUID id, String title, String destination,
                            LocalDate startDate, LocalDate endDate, String color,
                            double latitude, double longitude,
                            String ownerId, String ownerName, String ownerProfilePicture,
                            boolean own) {
        this.id = id;
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.color = color;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerProfilePicture = ownerProfilePicture;
        this.own = own;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerProfilePicture() { return ownerProfilePicture; }
    public void setOwnerProfilePicture(String ownerProfilePicture) { this.ownerProfilePicture = ownerProfilePicture; }

    public boolean isOwn() { return own; }
    public void setOwn(boolean own) { this.own = own; }
}
