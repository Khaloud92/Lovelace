package com.sep.sep_backend.emailcost;

import com.sep.sep_backend.budget.Budget;
import com.sep.sep_backend.budget.BudgetRepository;
import com.sep.sep_backend.cost.Cost;
import com.sep.sep_backend.cost.CostRepository;
import com.sep.sep_backend.emailcost.dto.EmailCostImportRequest;
import com.sep.sep_backend.emailcost.dto.EmailCostResponse;
import com.sep.sep_backend.emailcost.dto.FetchEmailCostsRequest;
import com.sep.sep_backend.emailcost.mailbox.MailboxConnection;
import com.sep.sep_backend.emailcost.mailbox.MailboxConnectionRepository;
import com.sep.sep_backend.emailcost.mailbox.MailboxEmailMessage;
import com.sep.sep_backend.emailcost.mailbox.MailboxReaderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Imports travel costs from a connected mailbox and keeps them in sync
 * with the budget costs of the assigned trip.
 */
@Service
public class EmailCostService {

    private final EmailCostRepository emailCostRepository;
    private final MailboxConnectionRepository mailboxConnectionRepository;
    private final EmailCostParserService emailCostParserService;
    private final BudgetRepository budgetRepository;
    private final CostRepository costRepository;
    private final MailboxReaderService mailboxReaderService;

    public EmailCostService(
            EmailCostRepository emailCostRepository,
            MailboxConnectionRepository mailboxConnectionRepository,
            EmailCostParserService emailCostParserService,
            BudgetRepository budgetRepository,
            CostRepository costRepository,
            MailboxReaderService mailboxReaderService
    ) {
        this.emailCostRepository = emailCostRepository;
        this.mailboxConnectionRepository = mailboxConnectionRepository;
        this.emailCostParserService = emailCostParserService;
        this.budgetRepository = budgetRepository;
        this.costRepository = costRepository;
        this.mailboxReaderService = mailboxReaderService;
    }

    /**
     * Returns the user's non-ignored email costs, newest first.
     *
     * @param userId        id of the user
     * @param travelEntryId optional trip filter; null returns all trips
     * @return the matching email costs
     */
    public List<EmailCostResponse> getEmailCosts(String userId, UUID travelEntryId) {
        if (travelEntryId != null) {
            return emailCostRepository
                    .findByUserIdAndTravelEntryIdAndIgnoredFalseOrderByImportedAtDesc(userId, travelEntryId)
                    .stream()
                    .map(EmailCostResponse::fromEntity)
                    .toList();
        }

        return emailCostRepository.findByUserIdAndIgnoredFalseOrderByImportedAtDesc(userId)
                .stream()
                .map(EmailCostResponse::fromEntity)
                .toList();
    }

    /** Alias for {@link #getEmailCosts(String, UUID)}. */
    public List<EmailCostResponse> getCosts(String userId, UUID travelEntryId) {
        return getEmailCosts(userId, travelEntryId);
    }

    /**
     * Reads recent mails from the user's connected mailbox, imports cost-like
     * mails as email costs, and mirrors them into the selected trip's budget.
     *
     * @param userId  id of the user
     * @param request optional trip selection
     * @return the user's email costs after the import
     */
    @Transactional
    public List<EmailCostResponse> fetchCostsFromConnectedMailbox(String userId, FetchEmailCostsRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        UUID selectedTravelEntryId = request != null ? request.getTravelEntryId() : null;

        if (selectedTravelEntryId != null &&
                budgetRepository.findByTravelEntry_Id(selectedTravelEntryId).isEmpty()) {
            throw new IllegalArgumentException("No budget found for this travel entry");
        }

        MailboxConnection connection = mailboxConnectionRepository
                .findFirstByUserIdAndActiveTrueOrderByConnectedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("Mailbox is not connected"));

        List<MailboxEmailMessage> messages = mailboxReaderService.fetchRecentMessages(connection);

        for (MailboxEmailMessage message : messages) {
            String externalMessageId = message.getExternalMessageId();

            if (externalMessageId != null) {
                boolean handledExistingCost = handleExistingEmailCost(
                        userId,
                        externalMessageId,
                        selectedTravelEntryId
                );

                if (handledExistingCost) {
                    continue;
                }
            }

            String subject = message.getSubject() != null ? message.getSubject() : "";
            String text = message.getText() != null ? message.getText() : "";

            String fullEmailText = subject + "\n" + text;

            if (!emailCostParserService.looksLikeRealCostEmail(fullEmailText)) {
                continue;
            }

            try {
                EmailCost emailCost = emailCostParserService.buildEmailCostFromText(
                        subject,
                        text,
                        "GMAIL",
                        externalMessageId,
                        userId,
                        selectedTravelEntryId
                );

                emailCost.setIgnored(false);

                EmailCost savedEmailCost = emailCostRepository.save(emailCost);
                createBudgetCostIfMissing(savedEmailCost, selectedTravelEntryId);
            } catch (IllegalArgumentException exception) {
                // Ignore emails that look relevant but do not contain a usable amount.
            }
        }

        return getEmailCosts(userId, selectedTravelEntryId);
    }

    /**
     * Creates an email cost from manually pasted email text.
     *
     * @param request the email text and target trip
     * @return the created email cost
     */
    @Transactional
    public EmailCostResponse createFromEmailText(EmailCostImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        EmailCost emailCost = emailCostParserService.buildEmailCostFromText(
                request.getSubject(),
                request.getEmailText(),
                request.getSource(),
                null,
                request.getUserId(),
                request.getTravelEntryId()
        );

        emailCost.setIgnored(false);

        EmailCost savedEmailCost = emailCostRepository.save(emailCost);
        createBudgetCostIfMissing(savedEmailCost, request.getTravelEntryId());

        return EmailCostResponse.fromEntity(savedEmailCost);
    }

    /**
     * Detaches an email cost from its trip and removes the mirrored budget cost.
     *
     * @param userId      id of the user
     * @param emailCostId id of the email cost
     */
    @Transactional
    public void removeEmailCostFromTrip(String userId, UUID emailCostId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        EmailCost emailCost = emailCostRepository.findByIdAndUserId(emailCostId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Email cost not found"));

        removeBudgetCostIfExists(emailCost, emailCost.getTravelEntryId());

        emailCost.setTravelEntryId(null);
        emailCost.setIgnored(false);

        emailCostRepository.save(emailCost);
    }

    /**
     * Marks an email cost as ignored so future mailbox imports skip it.
     *
     * @param userId      id of the user
     * @param emailCostId id of the email cost
     */
    @Transactional
    public void ignoreEmailCostForever(String userId, UUID emailCostId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        EmailCost emailCost = emailCostRepository.findByIdAndUserId(emailCostId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Email cost not found"));

        removeBudgetCostIfExists(emailCost, emailCost.getTravelEntryId());

        emailCost.setIgnored(true);
        emailCost.setTravelEntryId(null);

        emailCostRepository.save(emailCost);
    }

    private boolean handleExistingEmailCost(String userId, String externalMessageId, UUID selectedTravelEntryId) {
        return emailCostRepository
                .findFirstByUserIdAndExternalMessageIdOrderByImportedAtDesc(userId, externalMessageId)
                .map(existingEmailCost -> {
                    if (existingEmailCost.isIgnored()) {
                        return true;
                    }

                    if (selectedTravelEntryId == null) {
                        return true;
                    }

                    UUID oldTravelEntryId = existingEmailCost.getTravelEntryId();
                    boolean travelEntryChanged = !selectedTravelEntryId.equals(oldTravelEntryId);

                    if (travelEntryChanged) {
                        removeBudgetCostIfExists(existingEmailCost, oldTravelEntryId);

                        existingEmailCost.setTravelEntryId(selectedTravelEntryId);
                        existingEmailCost.setIgnored(false);

                        EmailCost savedEmailCost = emailCostRepository.save(existingEmailCost);
                        createBudgetCostIfMissing(savedEmailCost, selectedTravelEntryId);

                        return true;
                    }

                    createBudgetCostIfMissing(existingEmailCost, selectedTravelEntryId);
                    return true;
                })
                .orElse(false);
    }

    private void createBudgetCostIfMissing(EmailCost emailCost, UUID travelEntryId) {
        if (travelEntryId == null || emailCost.isIgnored()) {
            return;
        }

        Budget budget = budgetRepository.findByTravelEntry_Id(travelEntryId)
                .orElseThrow(() -> new IllegalArgumentException("No budget found for this travel entry"));

        Double amount = emailCost.getAmount().doubleValue();

        boolean alreadyExists = costRepository.existsByBudgetAndNameAndAmount(
                budget,
                emailCost.getTitle(),
                amount
        );

        if (alreadyExists) {
            return;
        }

        Cost cost = new Cost();
        cost.setBudget(budget);
        cost.setName(emailCost.getTitle());
        cost.setAmount(amount);

        costRepository.save(cost);
    }

    private void removeBudgetCostIfExists(EmailCost emailCost, UUID oldTravelEntryId) {
        if (oldTravelEntryId == null) {
            return;
        }

        budgetRepository.findByTravelEntry_Id(oldTravelEntryId)
                .flatMap(oldBudget -> costRepository.findFirstByBudgetAndNameAndAmount(
                        oldBudget,
                        emailCost.getTitle(),
                        emailCost.getAmount().doubleValue()
                ))
                .ifPresent(costRepository::delete);
    }
}