package com.sep.sep_backend.emailcost;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostRepository;
import com.sep.sep_backend.emailcost.dto.EmailCostResponse;
import com.sep.sep_backend.emailcost.dto.FetchEmailCostsRequest;
import com.sep.sep_backend.emailcost.mailbox.MailboxConnection;
import com.sep.sep_backend.emailcost.mailbox.MailboxConnectionRepository;
import com.sep.sep_backend.emailcost.mailbox.MailboxEmailMessage;
import com.sep.sep_backend.emailcost.mailbox.MailboxReaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailCostServiceTest {

    @Mock
    private EmailCostRepository emailCostRepository;

    @Mock
    private MailboxConnectionRepository mailboxConnectionRepository;

    @Mock
    private EmailCostParserService emailCostParserService;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CostRepository costRepository;

    @Mock
    private MailboxReaderService mailboxReaderService;

    private EmailCostService emailCostService;

    @BeforeEach
    void setUp() {
        emailCostService = new EmailCostService(
                emailCostRepository,
                mailboxConnectionRepository,
                emailCostParserService,
                budgetRepository,
                costRepository,
                mailboxReaderService
        );
    }

    @Test
    void fetchCostsFromConnectedMailbox_shouldImportNewEmailCostAndCreateBudgetCost() {
        String userId = "user-1";
        UUID travelEntryId = UUID.randomUUID();

        FetchEmailCostsRequest request = new FetchEmailCostsRequest();
        request.setTravelEntryId(travelEntryId);

        Budget budget = new Budget();

        MailboxConnection connection = new MailboxConnection();
        connection.setUserId(userId);
        connection.setEmailAddress("test@gmail.com");
        connection.setActive(true);

        MailboxEmailMessage message = new MailboxEmailMessage(
                "message-1",
                "Taxi ride receipt",
                "Your taxi ride total amount is 27.40 EUR."
        );

        EmailCost parsedEmailCost = new EmailCost();
        parsedEmailCost.setTitle("Taxi ride receipt");
        parsedEmailCost.setAmount(new BigDecimal("27.40"));
        parsedEmailCost.setCurrency("EUR");
        parsedEmailCost.setCategory("Transport");
        parsedEmailCost.setSubject("Taxi ride receipt");
        parsedEmailCost.setSource("GMAIL");
        parsedEmailCost.setExternalMessageId("message-1");
        parsedEmailCost.setEmailText("Your taxi ride total amount is 27.40 EUR.");
        parsedEmailCost.setUserId(userId);
        parsedEmailCost.setTravelEntryId(travelEntryId);
        parsedEmailCost.setIgnored(false);

        when(budgetRepository.findByTravelEntry_Id(travelEntryId))
                .thenReturn(Optional.of(budget));

        when(mailboxConnectionRepository.findFirstByUserIdAndActiveTrueOrderByConnectedAtDesc(userId))
                .thenReturn(Optional.of(connection));

        when(mailboxReaderService.fetchRecentMessages(connection))
                .thenReturn(List.of(message));

        when(emailCostRepository.findFirstByUserIdAndExternalMessageIdOrderByImportedAtDesc(userId, "message-1"))
                .thenReturn(Optional.empty());

        when(emailCostParserService.looksLikeRealCostEmail(anyString()))
                .thenReturn(true);

        when(emailCostParserService.buildEmailCostFromText(
                anyString(),
                anyString(),
                eq("GMAIL"),
                eq("message-1"),
                eq(userId),
                eq(travelEntryId)
        )).thenReturn(parsedEmailCost);

        when(emailCostRepository.save(any(EmailCost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(costRepository.existsByBudgetAndNameAndAmount(
                eq(budget),
                eq("Taxi ride receipt"),
                eq(27.40)
        )).thenReturn(false);

        when(emailCostRepository.findByUserIdAndTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(userId, travelEntryId))
                .thenReturn(List.of(parsedEmailCost));

        List<EmailCostResponse> result = emailCostService.fetchCostsFromConnectedMailbox(userId, request);

        assertEquals(1, result.size());
        assertEquals("Taxi ride receipt", result.get(0).getTitle());

        verify(emailCostRepository).save(parsedEmailCost);
        verify(costRepository).save(any(Cost.class));
    }

    @Test
    void fetchCostsFromConnectedMailbox_shouldSkipIgnoredExistingEmailCost() {
        String userId = "user-1";
        UUID travelEntryId = UUID.randomUUID();

        FetchEmailCostsRequest request = new FetchEmailCostsRequest();
        request.setTravelEntryId(travelEntryId);

        Budget budget = new Budget();

        MailboxConnection connection = new MailboxConnection();
        connection.setUserId(userId);
        connection.setEmailAddress("test@gmail.com");
        connection.setActive(true);

        MailboxEmailMessage message = new MailboxEmailMessage(
                "message-ignored",
                "Hotel receipt",
                "Total amount 100.00 EUR."
        );

        EmailCost ignoredEmailCost = new EmailCost();
        ignoredEmailCost.setTitle("Hotel receipt");
        ignoredEmailCost.setAmount(new BigDecimal("100.00"));
        ignoredEmailCost.setCurrency("EUR");
        ignoredEmailCost.setUserId(userId);
        ignoredEmailCost.setExternalMessageId("message-ignored");
        ignoredEmailCost.setIgnored(true);

        when(budgetRepository.findByTravelEntry_Id(travelEntryId))
                .thenReturn(Optional.of(budget));

        when(mailboxConnectionRepository.findFirstByUserIdAndActiveTrueOrderByConnectedAtDesc(userId))
                .thenReturn(Optional.of(connection));

        when(mailboxReaderService.fetchRecentMessages(connection))
                .thenReturn(List.of(message));

        when(emailCostRepository.findFirstByUserIdAndExternalMessageIdOrderByImportedAtDesc(userId, "message-ignored"))
                .thenReturn(Optional.of(ignoredEmailCost));

        when(emailCostRepository.findByUserIdAndTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(userId, travelEntryId))
                .thenReturn(List.of());

        List<EmailCostResponse> result = emailCostService.fetchCostsFromConnectedMailbox(userId, request);

        assertTrue(result.isEmpty());

        verify(emailCostParserService, never()).buildEmailCostFromText(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any()
        );

        verify(costRepository, never()).save(any(Cost.class));
    }

    @Test
    void removeEmailCostFromTrip_shouldRemoveBudgetCostAndKeepEmailCostImportableAgain() {
        String userId = "user-1";
        UUID emailCostId = UUID.randomUUID();
        UUID travelEntryId = UUID.randomUUID();

        Budget budget = new Budget();
        Cost cost = new Cost();

        EmailCost emailCost = new EmailCost();
        emailCost.setTitle("Taxi ride receipt");
        emailCost.setAmount(new BigDecimal("27.40"));
        emailCost.setCurrency("EUR");
        emailCost.setUserId(userId);
        emailCost.setTravelEntryId(travelEntryId);
        emailCost.setIgnored(false);

        when(emailCostRepository.findByIdAndUserId(emailCostId, userId))
                .thenReturn(Optional.of(emailCost));

        when(budgetRepository.findByTravelEntry_Id(travelEntryId))
                .thenReturn(Optional.of(budget));

        when(costRepository.findFirstByBudgetAndNameAndAmount(
                budget,
                "Taxi ride receipt",
                27.40
        )).thenReturn(Optional.of(cost));

        emailCostService.removeEmailCostFromTrip(userId, emailCostId);

        assertNull(emailCost.getTravelEntryId());
        assertFalse(emailCost.isIgnored());

        verify(costRepository).delete(cost);
        verify(emailCostRepository).save(emailCost);
    }

    @Test
    void ignoreEmailCostForever_shouldRemoveBudgetCostAndMarkEmailCostAsIgnored() {
        String userId = "user-1";
        UUID emailCostId = UUID.randomUUID();
        UUID travelEntryId = UUID.randomUUID();

        Budget budget = new Budget();
        Cost cost = new Cost();

        EmailCost emailCost = new EmailCost();
        emailCost.setTitle("Hotel receipt");
        emailCost.setAmount(new BigDecimal("120.00"));
        emailCost.setCurrency("EUR");
        emailCost.setUserId(userId);
        emailCost.setTravelEntryId(travelEntryId);
        emailCost.setIgnored(false);

        when(emailCostRepository.findByIdAndUserId(emailCostId, userId))
                .thenReturn(Optional.of(emailCost));

        when(budgetRepository.findByTravelEntry_Id(travelEntryId))
                .thenReturn(Optional.of(budget));

        when(costRepository.findFirstByBudgetAndNameAndAmount(
                budget,
                "Hotel receipt",
                120.00
        )).thenReturn(Optional.of(cost));

        emailCostService.ignoreEmailCostForever(userId, emailCostId);

        assertNull(emailCost.getTravelEntryId());
        assertTrue(emailCost.isIgnored());

        verify(costRepository).delete(cost);
        verify(emailCostRepository).save(emailCost);
    }

    @Test
    void getEmailCosts_shouldReturnOnlyNotIgnoredCostsForSelectedTrip() {
        String userId = "user-1";
        UUID travelEntryId = UUID.randomUUID();

        EmailCost emailCost = new EmailCost();
        emailCost.setTitle("Restaurant receipt");
        emailCost.setAmount(new BigDecimal("38.90"));
        emailCost.setCurrency("EUR");
        emailCost.setCategory("Food");
        emailCost.setUserId(userId);
        emailCost.setTravelEntryId(travelEntryId);
        emailCost.setIgnored(false);

        when(emailCostRepository.findByUserIdAndTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(userId, travelEntryId))
                .thenReturn(List.of(emailCost));

        List<EmailCostResponse> result = emailCostService.getEmailCosts(userId, travelEntryId);

        assertEquals(1, result.size());
        assertEquals("Restaurant receipt", result.get(0).getTitle());

        verify(emailCostRepository).findByUserIdAndTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(
                userId,
                travelEntryId
        );
    }
}