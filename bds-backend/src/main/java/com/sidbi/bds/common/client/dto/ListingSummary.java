package com.sidbi.bds.common.client.dto;

public record ListingSummary(
        Long id,
        String referenceNo,
        String title,
        Long sellerId,
        String sellerName,
        Long categoryId,
        String categoryName,
        Long subcategoryId,
        String subcategoryName,
        String deliveryMode,
        String status,
        boolean publiclyVisible
) {
}
