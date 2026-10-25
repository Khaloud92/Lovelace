package com.sep.sep_backend.emailcost;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailCostParserServiceTest {

    private final EmailCostParserService parserService = new EmailCostParserService();

    @Test
    void looksLikeRealCostEmail_shouldReturnTrueForTaxiReceipt() {
        String emailText = """
                Hello,
                thank you for using City Taxi Service.
                This is your transport receipt for your taxi ride.
                Total amount: 27.40 EUR.
                Payment was completed successfully.
                """;

        boolean result = parserService.looksLikeRealCostEmail(emailText);

        assertTrue(result);
    }

    @Test
    void buildEmailCostFromText_shouldExtractAmountAndBasicFields() {
        UUID travelEntryId = UUID.randomUUID();

        EmailCost emailCost = parserService.buildEmailCostFromText(
                "Taxi ride receipt",
                "Your taxi ride total amount is 27.40 EUR. Payment completed.",
                "GMAIL",
                "message-123",
                "user-1",
                travelEntryId
        );

        assertNotNull(emailCost);
        assertEquals("user-1", emailCost.getUserId());
        assertEquals(travelEntryId, emailCost.getTravelEntryId());
        assertEquals("GMAIL", emailCost.getSource());
        assertEquals("message-123", emailCost.getExternalMessageId());
        assertEquals("EUR", emailCost.getCurrency());
        assertEquals(0, new BigDecimal("27.40").compareTo(emailCost.getAmount()));
        assertFalse(emailCost.isIgnored());
        assertNotNull(emailCost.getTitle());
        assertNotNull(emailCost.getCategory());
    }

    @Test
    void buildEmailCostFromText_shouldThrowExceptionWhenNoAmountExists() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parserService.buildEmailCostFromText(
                        "Random newsletter",
                        "This email has no price and no amount.",
                        "GMAIL",
                        "message-456",
                        "user-1",
                        UUID.randomUUID()
                )
        );
    }
}