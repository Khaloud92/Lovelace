package com.sep.sep_backend.cost;

import com.sep.sep_backend.cost.dto.request.CreateCostRequest;
import com.sep.sep_backend.cost.dto.request.UpdateCostRequest;
import com.sep.sep_backend.registration.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing cost items, including document-based cost scanning.
 */
@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CostController {

    private final CostService costService;
    private final CostDocumentScanService costDocumentScanService;
    private final AppUserRepository appUserRepository;

    /**
     * Creates a cost and attaches it to a budget.
     *
     * @param budgetId id of the budget
     * @param request  the cost data
     * @return the created cost, or an error response
     */
    @PostMapping("/budget/{budgetId}")
    public ResponseEntity<?> addCostToBudget(@PathVariable UUID budgetId, @Valid @RequestBody CreateCostRequest request) {
        try {
            Cost createdCost = costService.addCostToBudget(budgetId, request);
            return ResponseEntity.ok(createdCost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Extracts costs from an uploaded document and adds them to a budget.
     *
     * @param budgetId id of the budget
     * @param file     the document to scan
     * @return the extracted costs, or an error response
     */
    @PostMapping("/budget/{budgetId}/scan-document")
    public ResponseEntity<?> scanDocument(@PathVariable UUID budgetId, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Datei darf nicht leer sein.");
            }
            List<Cost> costs = costDocumentScanService.scanDocumentAndAddCosts(budgetId, file);
            return ResponseEntity.ok(costs);
        } catch (NoCostsFoundException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler beim Scannen des Dokuments: " + e.getMessage());
        }
    }

    /**
     * Returns all costs of the authenticated user.
     *
     * @return the user's costs
     */
    @GetMapping
    public List<Cost> getAll() {
        return costService.getAll(resolveUserId());
    }

    /**
     * Updates an existing cost.
     *
     * @param id      id of the cost
     * @param request the new values
     * @return the updated cost, or an error response
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateCostRequest request) {
        try {
            Cost updatedCost = costService.update(id, request);
            return ResponseEntity.ok(updatedCost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a cost.
     *
     * @param id id of the cost
     * @return 204 on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        costService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

}
