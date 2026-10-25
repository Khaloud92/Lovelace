package com.sep.sep_backend.emailcost.dto;

import com.sep.sep_backend.emailcost.EmailCost;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body representing an imported email cost.
 */
@Getter
@AllArgsConstructor
public class EmailCostResponse {

    private UUID id;

    private String title;

    private BigDecimal amount;

    private String currency;

    private String category;

    private String subject;

    private String source;

    private String externalMessageId;

    private String emailText;

    private String userId;

    private UUID travelEntryId;

    private LocalDateTime importedAt;

    private boolean ignored;

    public static EmailCostResponse fromEntity(EmailCost emailCost) {
        return new EmailCostResponse(
                emailCost.getId(),
                emailCost.getTitle(),
                emailCost.getAmount(),
                emailCost.getCurrency(),
                emailCost.getCategory(),
                emailCost.getSubject(),
                emailCost.getSource(),
                emailCost.getExternalMessageId(),
                emailCost.getEmailText(),
                emailCost.getUserId(),
                emailCost.getTravelEntryId(),
                emailCost.getImportedAt(),
                emailCost.isIgnored()
        );
    }
}