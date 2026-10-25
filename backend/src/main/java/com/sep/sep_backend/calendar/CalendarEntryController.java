package com.sep.sep_backend.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.sep_backend.calendar.dto.CalendarEntryResponse;
import com.sep.sep_backend.calendar.dto.CreateCalendarEntryRequest;
import com.sep.sep_backend.calendar.dto.UpdateCalendarEntryRequest;
import com.sep.sep_backend.registration.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing calendar entries.
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/calendar")
public class CalendarEntryController {

    private final CalendarEntryService service;
    private final AppUserRepository appUserRepository;

    public CalendarEntryController(CalendarEntryService service, AppUserRepository appUserRepository) {
        this.service = service;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns all calendar entries of the authenticated user.
     *
     * @return the user's calendar entries
     */
    @GetMapping
    public ResponseEntity<List<CalendarEntryResponse>> getAllEntries() {
        return ResponseEntity.ok(service.getAllEntries(resolveUserId()));
    }

    /**
     * Creates a calendar entry for the authenticated user.
     *
     * @param request the entry data
     * @return the created entry
     */
    @PostMapping
    public ResponseEntity<CalendarEntryResponse> createEntry(
            @Valid @RequestBody CreateCalendarEntryRequest request) {
        CalendarEntryResponse createdEntry = service.createEntry(request, resolveUserId());
        return ResponseEntity
                .created(URI.create("/api/calendar/" + createdEntry.getId()))
                .body(createdEntry);
    }

    /**
     * Updates a calendar entry.
     *
     * @param id      id of the entry
     * @param request the new values
     * @return the updated entry
     */
    @PutMapping("/{id}")
    public ResponseEntity<CalendarEntryResponse> updateEntry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCalendarEntryRequest request) {
        return ResponseEntity.ok(service.updateEntry(id, request));
    }

    /**
     * Deletes a calendar entry.
     *
     * @param id id of the entry
     * @return 204 on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable UUID id) {
        service.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @Autowired
    private DocumentScannerService documentScannerService;

    /**
     * Extracts trip data from an uploaded ticket PDF and creates a calendar entry from it.
     *
     * @param file the ticket PDF
     * @return the created entry, or an error response
     */
    @PostMapping("/upload-ticket")
    public ResponseEntity<?> uploadTicket(@RequestParam("file") MultipartFile file) {
        try {
            String aiJsonResult = documentScannerService.scanAndExtractData(file);

            // Strip markdown fences and append the UTC 'Z' the model sometimes omits on timestamps.
            aiJsonResult = aiJsonResult.replace("```json", "").replace("```", "").trim();
            aiJsonResult = aiJsonResult.replaceAll("(\\d{2}:\\d{2}:\\d{2})(\")", "$1Z$2");

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            CalendarEntry aiEntry = mapper.readValue(aiJsonResult, CalendarEntry.class);

            CreateCalendarEntryRequest requestForService = new CreateCalendarEntryRequest();
            requestForService.setTitle(aiEntry.getTitle());
            requestForService.setStartDate(aiEntry.getStartDate());
            requestForService.setEndDate(aiEntry.getEndDate());
            requestForService.setLocation(aiEntry.getLocation());
            requestForService.setColor("#22d3ee");

            return ResponseEntity.ok(service.createEntry(requestForService, resolveUserId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Fehler beim KI-Scan: " + e.getMessage());
        }
    }

    private String resolveUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
