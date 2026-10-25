package com.sep.sep_backend.cost.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * JSON structure the AI model returns when extracting costs from a document.
 *
 * @param costs the extracted cost items
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiCostListResponse(List<AiCostItem> costs) {

    /**
     * A single extracted cost.
     *
     * @param name   merchant or provider name
     * @param amount total amount in euros
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiCostItem(String name, Double amount) {
    }
}
