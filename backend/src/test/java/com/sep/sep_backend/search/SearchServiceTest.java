package com.sep.sep_backend.search;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.calendar.CalendarEntryRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostRepository;
import com.sep.sep_backend.emailcost.EmailCost;
import com.sep.sep_backend.emailcost.EmailCostRepository;
import com.sep.sep_backend.search.dto.SearchResultDto;
import com.sep.sep_backend.search.dto.SearchSuggestionDto;
import com.sep.sep_backend.travelentry.TravelEntry;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    @Mock
    private TravelEntryRepository travelEntryRepository;

    @Mock
    private CalendarEntryRepository calendarEntryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CostRepository costRepository;

    @Mock
    private EmailCostRepository emailCostRepository;

    @Mock
    private SearchQueryRepository searchQueryRepository;

    @InjectMocks
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchWithBlankQueryReturnsNoResultsAndDoesNotSaveQuery() {
        List<SearchResultDto> results = searchService.search("user-1", "   ");

        assertThat(results).isEmpty();
        verify(searchQueryRepository, never()).save(any(SearchQuery.class));
    }

    @Test
    void searchReturnsMatchingTravelEntryAndStoresCleanedQuery() {
        String userId = "user-1";
        mockEmptyDomainResults(userId);

        TravelEntry travelEntry = new TravelEntry();
        travelEntry.setId(UUID.randomUUID());
        travelEntry.setTitle("Paris Trip");
        travelEntry.setDestination("Paris");
        travelEntry.setDescription("Summer vacation");

        when(travelEntryRepository.findByUserId(userId))
                .thenReturn(List.of(travelEntry));
        when(searchQueryRepository.findByUserIdAndQueryTextIgnoreCase(userId, "pa"))
                .thenReturn(Optional.empty());
        when(searchQueryRepository.save(any(SearchQuery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<SearchResultDto> results = searchService.search(userId, " pa ");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Paris Trip");
        assertThat(results.get(0).getSubtitle()).isEqualTo("Paris");
        assertThat(results.get(0).getType()).isEqualTo("Trip");
        assertThat(results.get(0).getRoute()).isEqualTo("/dashboard");

        ArgumentCaptor<SearchQuery> captor =
                ArgumentCaptor.forClass(SearchQuery.class);

        verify(searchQueryRepository).save(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getQueryText()).isEqualTo("pa");
        assertThat(captor.getValue().getSearchCount()).isEqualTo(1);
    }

    @Test
    void searchReturnsBudgetCostAndEmailCostResults() {
        String userId = "user-1";
        mockEmptyDomainResults(userId);

        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setName("Hotel Budget");
        budget.setAmount(500.0);

        Cost cost = new Cost();
        cost.setId(UUID.randomUUID());
        cost.setName("Hotel Tax");
        cost.setAmount(35.0);

        EmailCost emailCost = new EmailCost();
        emailCost.setId(UUID.randomUUID());
        emailCost.setTitle("Hotel invoice");
        emailCost.setSubject("Booking confirmation");
        emailCost.setCategory("Accommodation");
        emailCost.setSource("Email");

        when(budgetRepository.findByTravelEntry_UserId(userId))
                .thenReturn(List.of(budget));
        when(costRepository.findByBudget_TravelEntry_UserId(userId))
                .thenReturn(List.of(cost));
        when(emailCostRepository.findByUserIdAndIgnoredFalseOrderByImportedAtDesc(userId))
                .thenReturn(List.of(emailCost));
        when(searchQueryRepository.findByUserIdAndQueryTextIgnoreCase(userId, "hotel"))
                .thenReturn(Optional.empty());
        when(searchQueryRepository.save(any(SearchQuery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<SearchResultDto> results = searchService.search(userId, "hotel");

        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(SearchResultDto::getType)
                .contains("Budget", "Cost", "Email Cost");
    }

    @Test
    void getSuggestionsReturnsMappedSearchSuggestions() {
        String userId = "user-1";

        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setUserId(userId);
        searchQuery.setQueryText("Paris");
        searchQuery.setSearchCount(4);

        when(searchQueryRepository
                .findTop5ByUserIdOrderBySearchCountDescLastSearchedAtDesc(userId))
                .thenReturn(List.of(searchQuery));

        List<SearchSuggestionDto> suggestions =
                searchService.getSuggestions(userId);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getQueryText()).isEqualTo("Paris");
        assertThat(suggestions.get(0).getCount()).isEqualTo(4);
    }

    @Test
    void deleteSuggestionDeletesCleanedQuery() {
        searchService.deleteSuggestion("user-1", "  Paris  ");

        verify(searchQueryRepository)
                .deleteByUserIdAndQueryTextIgnoreCase("user-1", "Paris");
    }

    private void mockEmptyDomainResults(String userId) {
        when(travelEntryRepository.findByUserId(userId)).thenReturn(List.of());
        when(calendarEntryRepository.findByUserId(userId)).thenReturn(List.of());
        when(budgetRepository.findByTravelEntry_UserId(userId)).thenReturn(List.of());
        when(costRepository.findByBudget_TravelEntry_UserId(userId)).thenReturn(List.of());
        when(emailCostRepository.findByUserIdAndIgnoredFalseOrderByImportedAtDesc(userId))
                .thenReturn(List.of());
    }
}