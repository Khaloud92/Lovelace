package com.sep.sep_backend.search;

import com.sep.sep_backend.registration.AppUserRepository;
import com.sep.sep_backend.search.dto.SearchResultDto;
import com.sep.sep_backend.search.dto.SearchSuggestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for global search and search suggestions.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final AppUserRepository appUserRepository;

    /**
     * Searches the authenticated user's data.
     *
     * @param query the search text
     * @return the matching results
     */
    @GetMapping
    public ResponseEntity<List<SearchResultDto>> search(
            Authentication authentication,
            @RequestParam(required = false) String query
    ) {
        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(searchService.search(userId, query));
    }

    /**
     * Returns the user's most used previous searches.
     *
     * @return the suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestionDto>> getSuggestions(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(searchService.getSuggestions(userId));
    }

    /**
     * Deletes a saved search suggestion.
     *
     * @param query the suggestion text
     * @return 204 on success
     */
    @DeleteMapping("/suggestions")
    public ResponseEntity<Void> deleteSuggestion(
            Authentication authentication,
            @RequestParam String query
    ) {
        String userId = getCurrentUserId(authentication);
        searchService.deleteSuggestion(userId, query);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user");
        }

        String email = authentication.getName();

        return appUserRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}