package com.sep.sep_backend.travelentryTest;

import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.calendar.CalendarEntry;
import com.sep.sep_backend.calendar.CalendarEntryRepository;
import com.sep.sep_backend.travelentry.TravelEntryRepository;
import com.sep.sep_backend.travelentry.TravelEntryService;
import com.sep.sep_backend.travelentry.dto.CreateTravelEntryrequest;
import com.sep.sep_backend.travelentry.dto.TravelEntryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TravelCalendarIntegrationTest {

    @Autowired
    private TravelEntryService travelEntryService;

    @Autowired
    private TravelEntryRepository travelEntryRepository;

    @Autowired
    private CalendarEntryRepository calendarEntryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Test
    void createTravelEntry_shouldAlsoCreateCalendarEntry() {
        CreateTravelEntryrequest request = createRequest("Paris Trip", "Paris, France");

        travelEntryService.create(request);

        List<CalendarEntry> entries = calendarEntryRepository.findAll();
        assertEquals(1, entries.size());
        assertEquals("Paris Trip", entries.get(0).getTitle());
        assertEquals("Paris, France", entries.get(0).getLocation());
        assertNotNull(entries.get(0).getTravelEntry());
    }

    @Test
    void deleteTravelEntry_shouldAlsoDeleteCalendarEntry() {
        TravelEntryResponse response = travelEntryService.create(createRequest("Berlin Trip", "Berlin"));

        travelEntryService.delete(response.getId(), "user-1");

        assertTrue(travelEntryRepository.findAll().isEmpty());
        assertTrue(calendarEntryRepository.findAll().isEmpty());
    }

    @Test
    void deleteTravelEntry_shouldAlsoDeleteBudget() {
        CreateTravelEntryrequest request = createRequest("Rom Trip", "Rom");
        com.sep.sep_backend.budget.dto.request.CreateBudgetRequest budgetRequest =
                new com.sep.sep_backend.budget.dto.request.CreateBudgetRequest();
        budgetRequest.setName("Reisebudget Rom");
        budgetRequest.setAmount(1500.0);
        request.setBudget(budgetRequest);

        TravelEntryResponse response = travelEntryService.create(request);
        assertFalse(budgetRepository.findAll().isEmpty());

        travelEntryService.delete(response.getId(), "user-1");

        assertTrue(budgetRepository.findAll().isEmpty());
    }

    @Test
    void createTravelEntry_calendarEntry_shouldBeLinkedToCorrectTravelEntry() {
        TravelEntryResponse response = travelEntryService.create(createRequest("Madrid Trip", "Madrid"));

        List<CalendarEntry> entries = calendarEntryRepository.findAll();
        assertEquals(1, entries.size());
        assertNotNull(entries.get(0).getTravelEntry());
        assertEquals(response.getId(), entries.get(0).getTravelEntry().getId());
    }

    @Test
    void createTravelEntry_calendarEntry_shouldHaveCorrectDateAndColor() {
        travelEntryService.create(createRequest("Wien Trip", "Wien"));

        List<CalendarEntry> entries = calendarEntryRepository.findAll();
        assertEquals(1, entries.size());

        CalendarEntry calendarEntry = entries.get(0);
        assertEquals("#10b981", calendarEntry.getColor()); // default trip color
        assertEquals(2026, calendarEntry.getStartDate().getYear());
        assertEquals(6, calendarEntry.getStartDate().getMonthValue());
        assertEquals(1, calendarEntry.getStartDate().getDayOfMonth());
        assertEquals(2026, calendarEntry.getEndDate().getYear());
        assertEquals(6, calendarEntry.getEndDate().getMonthValue());
        assertEquals(7, calendarEntry.getEndDate().getDayOfMonth());
    }

    /**
     * Verifies that a calendar entry inherits the custom color
     * specified in the travel entry creation request.
     * When a color is explicitly provided, it should override
     * the default trip color (#10b981).
     */
    @Test
    void createTravelEntry_calendarEntry_shouldInheritCustomColor() {
        CreateTravelEntryrequest request = createRequest("Tokyo Trip", "Tokyo");
        request.setColor("#e11d48");

        travelEntryService.create(request);

        List<CalendarEntry> entries = calendarEntryRepository.findAll();
        assertEquals("#e11d48", entries.get(0).getColor());
    }

    /**
     * Creates a standard travel entry request for testing purposes.
     *
     * @param title       the title of the trip
     * @param destination the destination of the trip
     * @return a fully populated CreateTravelEntryrequest
     */
    private CreateTravelEntryrequest createRequest(String title, String destination) {
        CreateTravelEntryrequest request = new CreateTravelEntryrequest();
        request.setTitle(title);
        request.setDestination(destination);
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 7));
        request.setUserId("user-1");
        return request;
    }
}