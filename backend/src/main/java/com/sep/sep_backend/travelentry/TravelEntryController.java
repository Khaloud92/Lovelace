package com.sep.sep_backend.travelentry;

import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.travelentry.dto.TravelEntryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing travel entries.
 */
@RestController
@RequestMapping("/api/travel-entries")
@CrossOrigin(origins = "http://localhost:4200")
public class TravelEntryController {

    private final TravelEntryService service;
    private final AppUserRepository appUserRepository;

    public TravelEntryController(TravelEntryService service, AppUserRepository appUserRepository) {
        this.service = service;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns all travel entries of the authenticated user.
     *
     * @return the user's travel entries
     */
    @GetMapping
    public List<TravelEntryResponse> getTravelEntries() {
        return service.findAll(resolveUserId());
    }

    /**
     * Creates a travel entry for the authenticated user.
     *
     * @param request the trip data including its budget
     * @return the created entry, or an error response
     */
    @PostMapping
    public ResponseEntity<?> createTravelEntry(
            @jakarta.validation.Valid @RequestBody
            com.sep.sep_backend.travelentry.dto.CreateTravelEntryrequest request) {
        try {
            request.setUserId(resolveUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    /**
     * Updates a travel entry of the authenticated user.
     *
     * @param id          id of the entry
     * @param travelEntry the new values
     * @return the updated entry, or an error response
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTravelEntry(
            @PathVariable UUID id,
            @RequestBody TravelEntry travelEntry) {
        try {
            return ResponseEntity.ok(service.update(id, travelEntry, resolveUserId()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    /**
     * Deletes a travel entry of the authenticated user.
     *
     * @param id id of the entry
     * @return 204 on success, or an error response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTravelEntry(@PathVariable UUID id) {
        try {
            service.delete(id, resolveUserId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    private String resolveUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
