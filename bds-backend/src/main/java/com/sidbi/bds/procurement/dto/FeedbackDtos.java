package com.sidbi.bds.procurement.dto;

import com.sidbi.bds.procurement.domain.ProcEnums;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record QuestionRequest(
            @NotBlank @Size(max = 500) String questionText,
            @NotNull ProcEnums.QuestionType questionType,
            @Min(0) @Max(999) Integer displayOrder,
            Boolean mandatory,
            Boolean active
    ) {
    }

    public record QuestionResponse(Long id, String questionText, ProcEnums.QuestionType questionType, int displayOrder,
                                   boolean mandatory, boolean active) {
    }

    public record AnswerRequest(
            @NotNull Long questionId,
            @Min(1) @Max(5) Integer ratingValue,
            Boolean booleanValue,
            @Size(max = 2000) String textValue
    ) {
    }

    public record FeedbackRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 200) String title,
            @Size(max = 2000) String reviewText,
            @Valid @Size(max = 50) List<AnswerRequest> answers
    ) {
    }

    public record AnswerResponse(Long questionId, String questionText, ProcEnums.QuestionType questionType, Integer ratingValue,
                                 Boolean booleanValue, String textValue) {
    }

    public record ReviewResponse(
            Long id,
            Long engagementId,
            String engagementNumber,
            String engagementTitle,
            Long sellerId,
            String sellerName,
            String buyerName,
            int rating,
            boolean delivered,
            String title,
            String reviewText,
            boolean visible,
            List<AnswerResponse> answers,
            LocalDateTime createdAt
    ) {
    }

    public record RatingSummary(Long sellerId, double average, long count, Map<Integer, Long> distribution) {
    }

    public record VisibilityRequest(boolean visible, @Size(max = 500) String remarks) {
    }
}
