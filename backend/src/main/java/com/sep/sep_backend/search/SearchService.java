package com.sep.sep_backend.search;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.calendar.CalendarEntry;
import com.sep.sep_backend.calendar.CalendarEntryRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostRepository;
import com.sep.sep_backend.emailcost.EmailCost;
import com.sep.sep_backend.emailcost.EmailCostRepository;
import com.sep.sep_backend.search.dto.SearchResultDto;
import com.sep.sep_backend.search.dto.SearchSuggestionDto;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Global search over the user's trips, calendar entries, budgets, and costs,
 * plus suggestions based on previous searches.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_RESULTS = 20;

    private final TravelEntryRepository travelEntryRepository;
    private final CalendarEntryRepository calendarEntryRepository;
    private final BudgetRepository budgetRepository;
    private final CostRepository costRepository;
    private final EmailCostRepository emailCostRepository;
    private final SearchQueryRepository searchQueryRepository;

    /**
     * Searches all of the user's data for the query and records it for suggestions.
     *
     * @param userId id of the user
     * @param query  the search text
     * @return up to {@value MAX_RESULTS} matches
     */
    @Transactional
    public List<SearchResultDto> search(String userId, String query) {
        String cleanedQuery = cleanQuery(query);

        if (cleanedQuery.isBlank()) {
            return List.of();
        }

        saveSearchQuery(userId, cleanedQuery);

        List<SearchResultDto> results = new ArrayList<>();

        addTravelEntryResults(userId, cleanedQuery, results);
        addCalendarEntryResults(userId, cleanedQuery, results);
        addBudgetResults(userId, cleanedQuery, results);
        addCostResults(userId, cleanedQuery, results);
        addEmailCostResults(userId, cleanedQuery, results);

        return results.stream()
                .limit(MAX_RESULTS)
                .toList();
    }

    /**
     * Returns the user's five most used previous searches.
     *
     * @param userId id of the user
     * @return the suggestions
     */
    @Transactional(readOnly = true)
    public List<SearchSuggestionDto> getSuggestions(String userId) {
        return searchQueryRepository
                .findTop5ByUserIdOrderBySearchCountDescLastSearchedAtDesc(userId)
                .stream()
                .map(query -> new SearchSuggestionDto(
                        query.getQueryText(),
                        query.getSearchCount()
                ))
                .toList();
    }

    /**
     * Removes a saved search text from the user's suggestions.
     *
     * @param userId    id of the user
     * @param queryText the suggestion to delete
     */
    @Transactional
    public void deleteSuggestion(String userId, String queryText) {
        String cleanedQuery = cleanQuery(queryText);

        if (cleanedQuery.isBlank()) {
            return;
        }

        searchQueryRepository.deleteByUserIdAndQueryTextIgnoreCase(userId, cleanedQuery);
    }

    private void saveSearchQuery(String userId, String queryText) {
        SearchQuery searchQuery = searchQueryRepository
                .findByUserIdAndQueryTextIgnoreCase(userId, queryText)
                .orElseGet(() -> {
                    SearchQuery newSearchQuery = new SearchQuery();
                    newSearchQuery.setUserId(userId);
                    newSearchQuery.setQueryText(queryText);
                    newSearchQuery.setSearchCount(0);
                    return newSearchQuery;
                });

        searchQuery.setSearchCount(searchQuery.getSearchCount() + 1);
        searchQueryRepository.save(searchQuery);
    }

    private void addTravelEntryResults(
            String userId,
            String query,
            List<SearchResultDto> results
    ) {
        for (TravelEntry travelEntry : travelEntryRepository.findByUserId(userId)) {
            if (matches(
                    query,
                    travelEntry.getTitle(),
                    travelEntry.getDestination(),
                    travelEntry.getDescription()
            )) {
                results.add(new SearchResultDto(
                        travelEntry.getId().toString(),
                        travelEntry.getTitle(),
                        travelEntry.getDestination(),
                        "Trip",
                        "/dashboard"
                ));
            }
        }
    }

    private void addCalendarEntryResults(
            String userId,
            String query,
            List<SearchResultDto> results
    ) {
        for (CalendarEntry calendarEntry : calendarEntryRepository.findByUserId(userId)) {
            if (matches(
                    query,
                    calendarEntry.getTitle(),
                    calendarEntry.getLocation(),
                    calendarEntry.getDescription()
            )) {
                results.add(new SearchResultDto(
                        calendarEntry.getId().toString(),
                        calendarEntry.getTitle(),
                        calendarEntry.getLocation(),
                        "Calendar",
                        "/calendar"
                ));
            }
        }
    }

    private void addBudgetResults(
            String userId,
            String query,
            List<SearchResultDto> results
    ) {
        for (Budget budget : budgetRepository.findByTravelEntry_UserId(userId)) {
            if (matches(query, budget.getName())) {
                results.add(new SearchResultDto(
                        budget.getId().toString(),
                        budget.getName(),
                        formatAmount(budget.getAmount()),
                        "Budget",
                        "/budget"
                ));
            }
        }
    }

    private void addCostResults(
            String userId,
            String query,
            List<SearchResultDto> results
    ) {
        for (Cost cost : costRepository.findByBudget_TravelEntry_UserId(userId)) {
            if (matches(query, cost.getName())) {
                results.add(new SearchResultDto(
                        cost.getId().toString(),
                        cost.getName(),
                        formatAmount(cost.getAmount()),
                        "Cost",
                        "/budget"
                ));
            }
        }
    }

    private void addEmailCostResults(
            String userId,
            String query,
            List<SearchResultDto> results
    ) {
        for (EmailCost emailCost : emailCostRepository
                .findByUserIdAndIgnoredFalseOrderByImportedAtDesc(userId)) {
            if (matches(
                    query,
                    emailCost.getTitle(),
                    emailCost.getSubject(),
                    emailCost.getCategory(),
                    emailCost.getSource()
            )) {
                results.add(new SearchResultDto(
                        emailCost.getId().toString(),
                        emailCost.getTitle(),
                        emailCost.getSubject(),
                        "Email Cost",
                        "/email-costs"
                ));
            }
        }
    }

    private String cleanQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.trim().replaceAll("\\s+", " ");
    }

    private boolean matches(String query, String... values) {
        String lowerCaseQuery = query.toLowerCase();

        for (String value : values) {
            if (value != null && value.toLowerCase().contains(lowerCaseQuery)) {
                return true;
            }
        }

        return false;
    }

    private String formatAmount(Double amount) {
        if (amount == null) {
            return "";
        }

        return amount + " €";
    }
}