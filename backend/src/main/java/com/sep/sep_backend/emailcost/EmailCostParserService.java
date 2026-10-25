package com.sep.sep_backend.emailcost;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristically extracts cost data (amount, currency, category, title)
 * from email subjects and bodies using keyword and regex matching.
 */
@Service
public class EmailCostParserService {

    private static final int MAX_EMAIL_TEXT_LENGTH = 4000;

    private static final Pattern AMOUNT_WITH_CURRENCY_AFTER = Pattern.compile(
            "([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{1,6}(?:[.,][0-9]{1,2})?)\\s*(€|EUR|Euro|USD|\\$|GBP|£)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AMOUNT_WITH_CURRENCY_BEFORE = Pattern.compile(
            "(€|EUR|Euro|USD|\\$|GBP|£)\\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{1,6}(?:[.,][0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AMOUNT_AFTER_KEYWORD = Pattern.compile(
            "(?:total|price|amount|betrag|summe|kosten|paid|zahlung|gesamt|invoice|rechnung)\\D{0,40}([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]{1,6}(?:[.,][0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Builds an {@link EmailCost} from raw email content.
     *
     * @param subject           the email subject
     * @param emailText         the email body
     * @param source            mail provider label (e.g. GMAIL)
     * @param externalMessageId provider message id for deduplication, may be null
     * @param userId            id of the owning user
     * @param travelEntryId     optional trip assignment
     * @return the populated (unsaved) email cost
     * @throws IllegalArgumentException if no usable amount is found
     */
    public EmailCost buildEmailCostFromText(
            String subject,
            String emailText,
            String source,
            String externalMessageId,
            String userId,
            java.util.UUID travelEntryId
    ) {
        String normalizedSubject = Optional.ofNullable(subject).orElse("").trim();
        String normalizedEmailText = Optional.ofNullable(emailText).orElse("").trim();

        if (normalizedEmailText.isBlank()) {
            throw new IllegalArgumentException("Email text is required");
        }

        String fullText = normalizedSubject + " " + normalizedEmailText;

        EmailCost emailCost = new EmailCost();
        emailCost.setTitle(buildTitle(normalizedSubject, extractCategory(fullText)));
        emailCost.setAmount(extractAmount(fullText));
        emailCost.setCurrency(extractCurrency(fullText));
        emailCost.setCategory(extractCategory(fullText));
        emailCost.setSubject(normalizedSubject);
        emailCost.setSource(source);
        emailCost.setExternalMessageId(externalMessageId);
        emailCost.setEmailText(shortenEmailText(normalizedEmailText));
        emailCost.setUserId(userId);
        emailCost.setTravelEntryId(travelEntryId);
        return emailCost;
    }

    /** Returns true if the text contains booking/payment keywords and no marketing keywords. */
    public boolean looksLikeRealCostEmail(String text) {
        String lowerText = Optional.ofNullable(text).orElse("").toLowerCase();

        if (containsMarketingKeyword(lowerText)) {
            return false;
        }

        return containsCostKeyword(lowerText);
    }

    private boolean containsMarketingKeyword(String lowerText) {
        return lowerText.contains("angebot")
                || lowerText.contains("offer")
                || lowerText.contains("newsletter")
                || lowerText.contains("unsubscribe")
                || lowerText.contains("abmelden")
                || lowerText.contains("promo")
                || lowerText.contains("promotion")
                || lowerText.contains("sale")
                || lowerText.contains("rabatt")
                || lowerText.contains("deal")
                || lowerText.contains("werbung")
                || lowerText.contains("€/monat")
                || lowerText.contains("eur/monat");
    }

    private boolean containsCostKeyword(String lowerText) {
        return lowerText.contains("booking confirmation")
                || lowerText.contains("buchungsbestätigung")
                || lowerText.contains("reservation")
                || lowerText.contains("reservierung")
                || lowerText.contains("flight")
                || lowerText.contains("flug")
                || lowerText.contains("hotel")
                || lowerText.contains("ticket")
                || lowerText.contains("invoice")
                || lowerText.contains("rechnung")
                || lowerText.contains("receipt")
                || lowerText.contains("quittung")
                || lowerText.contains("payment")
                || lowerText.contains("zahlung")
                || lowerText.contains("paid")
                || lowerText.contains("bezahlt")
                || lowerText.contains("total")
                || lowerText.contains("summe")
                || lowerText.contains("betrag")
                || lowerText.contains("amount");
    }

    private BigDecimal extractAmount(String text) {
        Matcher afterMatcher = AMOUNT_WITH_CURRENCY_AFTER.matcher(text);

        if (afterMatcher.find()) {
            return toBigDecimal(afterMatcher.group(1));
        }

        Matcher beforeMatcher = AMOUNT_WITH_CURRENCY_BEFORE.matcher(text);

        if (beforeMatcher.find()) {
            return toBigDecimal(beforeMatcher.group(2));
        }

        Matcher keywordMatcher = AMOUNT_AFTER_KEYWORD.matcher(text);

        if (keywordMatcher.find()) {
            return toBigDecimal(keywordMatcher.group(1));
        }

        throw new IllegalArgumentException("No amount could be found in the email text");
    }

    private BigDecimal toBigDecimal(String value) {
        String cleanedValue = value.replace(" ", "").trim();

        int lastComma = cleanedValue.lastIndexOf(',');
        int lastDot = cleanedValue.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                cleanedValue = cleanedValue.replace(".", "").replace(",", ".");
            } else {
                cleanedValue = cleanedValue.replace(",", "");
            }
        } else if (lastComma >= 0) {
            int decimals = cleanedValue.length() - lastComma - 1;

            if (decimals == 2) {
                cleanedValue = cleanedValue.replace(",", ".");
            } else {
                cleanedValue = cleanedValue.replace(",", "");
            }
        } else if (lastDot >= 0) {
            int decimals = cleanedValue.length() - lastDot - 1;

            if (decimals != 2) {
                cleanedValue = cleanedValue.replace(".", "");
            }
        }

        return new BigDecimal(cleanedValue).setScale(2, RoundingMode.HALF_UP);
    }

    private String extractCurrency(String text) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("usd") || lowerText.contains("$")) {
            return "USD";
        }

        if (lowerText.contains("gbp") || lowerText.contains("£")) {
            return "GBP";
        }

        return "EUR";
    }

    private String extractCategory(String text) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("flight")
                || lowerText.contains("flug")
                || lowerText.contains("airline")
                || lowerText.contains("ticket")
                || lowerText.contains("boarding")) {
            return "Flight";
        }

        if (lowerText.contains("hotel")
                || lowerText.contains("booking")
                || lowerText.contains("accommodation")
                || lowerText.contains("room")
                || lowerText.contains("übernachtung")) {
            return "Hotel";
        }

        if (lowerText.contains("train")
                || lowerText.contains("bahn")
                || lowerText.contains("bus")
                || lowerText.contains("taxi")
                || lowerText.contains("uber")
                || lowerText.contains("transport")) {
            return "Transport";
        }

        if (lowerText.contains("restaurant")
                || lowerText.contains("food")
                || lowerText.contains("essen")
                || lowerText.contains("meal")
                || lowerText.contains("lunch")
                || lowerText.contains("dinner")) {
            return "Food";
        }

        return "Other";
    }

    private String buildTitle(String subject, String category) {
        if (subject != null && !subject.isBlank()) {
            return subject.length() > 80 ? subject.substring(0, 80) : subject;
        }

        return category + " cost from email";
    }

    private String shortenEmailText(String emailText) {
        if (emailText == null) {
            return "";
        }

        if (emailText.length() <= MAX_EMAIL_TEXT_LENGTH) {
            return emailText;
        }

        return emailText.substring(0, MAX_EMAIL_TEXT_LENGTH);
    }
}