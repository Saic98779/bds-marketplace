package com.sidbi.bds.procurement.dto;

import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record TrendPoint(String period, long value) {
    }

    public record MultiTrendPoint(String period, long rfqs, long quotations, long engagements) {
    }

    public record CountItem(String label, long count) {
    }

    public record SellerDashboard(
            long pendingRfqs,
            long totalInvitations,
            long quotationsSubmitted,
            long quotationsAccepted,
            long activeEngagements,
            long completedEngagements,
            long workOrdersAwaited,
            long openQueries,
            Double averageRating,
            Integer reviewCount,
            double responseRate,
            double selectionRate,
            List<TrendPoint> monthlyEnquiries,
            List<RfqDtos.RfqSummary> recentRfqs,
            List<QuotationDtos.QuotationResponse> recentQuotations,
            List<EngagementDtos.EngagementSummary> recentEngagements
    ) {
    }

    public record BuyerDashboard(
            long totalRfqs,
            long openRfqs,
            long quotationsReceived,
            long comparisonPending,
            long selectedProviders,
            long activeEngagements,
            long completedEngagements,
            long workOrderRequests,
            long deliveriesToConfirm,
            long feedbackPending,
            long openQueries,
            List<RfqDtos.RfqSummary> recentRfqs,
            List<EngagementDtos.EngagementSummary> recentEngagements
    ) {
    }

    public record ProcurementStats(
            long totalRfqs,
            long activeRfqs,
            long totalQuotations,
            long totalEngagements,
            long activeEngagements,
            long completedEngagements,
            double selectionRate,
            double averageQuotationsPerRfq,
            long openGrievances,
            long escalatedQueries,
            long overdueQueries,
            Map<String, Long> rfqsByStatus,
            Map<String, Long> quotationsByStatus,
            Map<String, Long> engagementsByStatus,
            List<CountItem> rfqsByCategory,
            List<CountItem> rfqsByState,
            List<MultiTrendPoint> monthlyTrend
    ) {
    }
}
