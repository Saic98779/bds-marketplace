package com.sidbi.bds.procurement.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.procurement.dto.FeedbackDtos;
import com.sidbi.bds.procurement.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/reviews")
@RequiredArgsConstructor
@Tag(name = "Public Reviews", description = "Eligible purchaser feedback for verified providers")
public class PublicReviewController {

    private final FeedbackService feedbackService;

    @GetMapping("/sellers/{sellerId}")
    @Operation(summary = "Published reviews for a provider")
    public ApiResponse<PageResponse<FeedbackDtos.ReviewResponse>> reviews(@PathVariable Long sellerId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(feedbackService.publicReviews(sellerId, page, size));
    }

    @GetMapping("/sellers/{sellerId}/summary")
    @Operation(summary = "Rating summary for a provider")
    public ApiResponse<FeedbackDtos.RatingSummary> summary(@PathVariable Long sellerId) {
        return ApiResponse.ok(feedbackService.summary(sellerId));
    }
}
