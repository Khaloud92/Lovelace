package com.sep.sep_backend.dashboard.dto;

/**
 * Aggregated numbers shown on the dashboard.
 *
 * @param totalTravelEntries     number of trips
 * @param uploadedDocumentsCount number of uploaded documents
 * @param totalBudget            sum of all budget amounts
 * @param upcomingCalendarEvents number of future calendar entries
 */
public record DashboardSummaryDto(
        int totalTravelEntries,
        int uploadedDocumentsCount,
        double totalBudget,
        int upcomingCalendarEvents
) {}