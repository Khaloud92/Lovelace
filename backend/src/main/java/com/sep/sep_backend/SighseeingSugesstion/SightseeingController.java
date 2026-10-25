package com.sep.sep_backend.SighseeingSugesstion;


import com.sep.sep_backend.cost.Cost;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing AI-driven sightseeing suggestions.
 */
@RestController
@RequestMapping("/api/sightseeing")
@CrossOrigin(origins = "http://localhost:4200")
public class SightseeingController {

    private final SightseeingService sightseeingService;

    public SightseeingController(SightseeingService sightseeingService) {
        this.sightseeingService = sightseeingService;
    }

    /**
     * Fetches the top 3 sightseeing suggestions for a specific travel entry as temporary cost objects.
     *
     * @param travelEntryId The UUID of the travel entry.
     * @return A list of 3 suggested Cost items, or an error message if the request fails.
     */
    @GetMapping("/suggestions/{travelEntryId}")
    public ResponseEntity<?> getSuggestions(@PathVariable UUID travelEntryId) {
        try {
            List<Cost> suggestions = sightseeingService.getTopThreeSightsForTravelEntry(travelEntryId);
            return ResponseEntity.ok(suggestions);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei der Kommunikation mit der KI: " + e.getMessage());
        }
    }
}