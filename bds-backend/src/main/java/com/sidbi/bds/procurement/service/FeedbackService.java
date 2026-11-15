package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.CatalogClient;
import com.sidbi.bds.common.client.IdentityClient;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.client.dto.RatingUpdate;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.FeedbackAnswer;
import com.sidbi.bds.procurement.domain.FeedbackQuestion;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.Review;
import com.sidbi.bds.procurement.dto.FeedbackDtos;
import com.sidbi.bds.procurement.repository.FeedbackQuestionRepository;
import com.sidbi.bds.procurement.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackQuestionRepository questionRepository;
    private final ReviewRepository reviewRepository;
    private final EngagementService engagementService;
    private final IdentityClient identityClient;
    private final CatalogClient catalogClient;
    private final NotificationClient notificationClient;
    private final ActorGuard guard;
    private final ProcMapper mapper;

    @Transactional(readOnly = true)
    public List<FeedbackDtos.QuestionResponse> activeQuestions() {
        return questionRepository.findByActiveTrueOrderByDisplayOrderAsc().stream().map(mapper::question).toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackDtos.QuestionResponse> allQuestions() {
        guard.admin(Permission.FEEDBACK_MANAGE);
        return questionRepository.findAllByOrderByDisplayOrderAsc().stream().map(mapper::question).toList();
    }

    @Transactional
    public FeedbackDtos.QuestionResponse createQuestion(FeedbackDtos.QuestionRequest request) {
        guard.admin(Permission.FEEDBACK_MANAGE);
        FeedbackQuestion question = new FeedbackQuestion();
        apply(question, request);
        questionRepository.save(question);
        notificationClient.audit("FEEDBACK_QUESTION_CREATED", "FEEDBACK_QUESTION", question.getId(), question.getQuestionText());
        return mapper.question(question);
    }

    @Transactional
    public FeedbackDtos.QuestionResponse updateQuestion(Long id, FeedbackDtos.QuestionRequest request) {
        guard.admin(Permission.FEEDBACK_MANAGE);
        FeedbackQuestion question = questionRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Question not found"));
        apply(question, request);
        notificationClient.audit("FEEDBACK_QUESTION_UPDATED", "FEEDBACK_QUESTION", id, question.getQuestionText());
        return mapper.question(question);
    }

    @Transactional
    public FeedbackDtos.ReviewResponse submit(Long engagementId, FeedbackDtos.FeedbackRequest request) {
        guard.buyer(Permission.FEEDBACK_SUBMIT);
        Engagement engagement = engagementService.ownedByBuyer(engagementId);
        if (engagement.getStatus() != ProcEnums.EngagementStatus.COMPLETED || !Boolean.TRUE.equals(engagement.getDeliveryConfirmed())) {
            throw BusinessException.conflict("Feedback can be submitted after you confirm that the work has been delivered");
        }
        if (engagement.isFeedbackSubmitted() || reviewRepository.findByEngagementId(engagementId).isPresent()) {
            throw BusinessException.conflict("Feedback has already been submitted for this engagement");
        }
        List<FeedbackQuestion> questions = questionRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Map<Long, FeedbackDtos.AnswerRequest> answers = request.answers() == null ? Map.of()
                : request.answers().stream().collect(Collectors.toMap(FeedbackDtos.AnswerRequest::questionId, Function.identity(), (a, b) -> b));
        Review review = new Review();
        review.setEngagementId(engagementId);
        review.setEngagementNumber(engagement.getEngagementNumber());
        review.setEngagementTitle(engagement.getTitle());
        review.setSellerId(engagement.getSellerId());
        review.setSellerName(engagement.getSellerName());
        review.setBuyerId(engagement.getBuyerId());
        review.setBuyerName(engagement.getBuyerName());
        review.setRating(request.rating());
        review.setDelivered(true);
        review.setTitle(request.title());
        review.setReviewText(request.reviewText());
        for (FeedbackQuestion question : questions) {
            FeedbackDtos.AnswerRequest answer = answers.get(question.getId());
            boolean answered = answer != null && switch (question.getQuestionType()) {
                case RATING -> answer.ratingValue() != null;
                case YES_NO -> answer.booleanValue() != null;
                case TEXT -> answer.textValue() != null && !answer.textValue().isBlank();
            };
            if (!answered) {
                if (question.isMandatory()) {
                    throw BusinessException.badRequest("Please answer: " + question.getQuestionText());
                }
                continue;
            }
            FeedbackAnswer entity = new FeedbackAnswer();
            entity.setReview(review);
            entity.setQuestionId(question.getId());
            entity.setQuestionText(question.getQuestionText());
            entity.setQuestionType(question.getQuestionType());
            entity.setRatingValue(question.getQuestionType() == ProcEnums.QuestionType.RATING ? answer.ratingValue() : null);
            entity.setBooleanValue(question.getQuestionType() == ProcEnums.QuestionType.YES_NO ? answer.booleanValue() : null);
            entity.setTextValue(question.getQuestionType() == ProcEnums.QuestionType.TEXT ? answer.textValue().trim() : null);
            review.getAnswers().add(entity);
        }
        reviewRepository.save(review);
        engagement.setFeedbackSubmitted(true);
        syncRating(engagement.getSellerId());
        notificationClient.notify(engagement.getSellerId(), "FEEDBACK_RECEIVED", "FEEDBACK", "/seller/reviews",
                Map.of("engagement", engagement.getEngagementNumber(), "title", engagement.getTitle(), "rating", String.valueOf(request.rating()),
                        "buyer", engagement.getBuyerName() == null ? "" : engagement.getBuyerName()));
        notificationClient.audit("FEEDBACK_SUBMITTED", "ENGAGEMENT", engagementId, request.rating() + " stars");
        return mapper.review(review, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackDtos.ReviewResponse> sellerReviews(int page, int size) {
        Long sellerId = CurrentUser.scopeId();
        Page<Review> result = sellerId == null
                ? reviewRepository.findAll(PageRequestFactory.newestFirst(page, size))
                : reviewRepository.findBySellerId(sellerId, PageRequestFactory.newestFirst(page, size));
        return PageResponse.of(result, r -> mapper.review(r, true));
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackDtos.ReviewResponse> publicReviews(Long sellerId, int page, int size) {
        Page<Review> result = reviewRepository.findBySellerIdAndVisibleTrue(sellerId, PageRequestFactory.newestFirst(page, size));
        return PageResponse.of(result, r -> mapper.review(r, false));
    }

    @Transactional(readOnly = true)
    public FeedbackDtos.RatingSummary summary(Long sellerId) {
        Object[] aggregate = reviewRepository.aggregate(sellerId).getFirst();
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }
        for (Object[] row : reviewRepository.distribution(sellerId)) {
            distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        double average = Math.round(((Number) aggregate[0]).doubleValue() * 10.0) / 10.0;
        return new FeedbackDtos.RatingSummary(sellerId, average, ((Number) aggregate[1]).longValue(), distribution);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackDtos.ReviewResponse> adminReviews(Long sellerId, int page, int size) {
        guard.admin(Permission.FEEDBACK_MANAGE);
        Page<Review> result = sellerId == null
                ? reviewRepository.findAllReviews(PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                : reviewRepository.findBySellerId(sellerId, PageRequestFactory.newestFirst(page, size));
        return PageResponse.of(result, r -> mapper.review(r, true));
    }

    @Transactional
    public FeedbackDtos.ReviewResponse setVisibility(Long id, FeedbackDtos.VisibilityRequest request) {
        guard.admin(Permission.FEEDBACK_MANAGE);
        Review review = reviewRepository.findWithAnswersById(id).orElseThrow(() -> BusinessException.notFound("Review not found"));
        review.setVisible(request.visible());
        review.setModerationRemarks(request.remarks());
        reviewRepository.flush();
        syncRating(review.getSellerId());
        notificationClient.audit(request.visible() ? "REVIEW_SHOWN" : "REVIEW_HIDDEN", "REVIEW", id, request.remarks());
        return mapper.review(review, true);
    }

    private void syncRating(Long sellerId) {
        reviewRepository.flush();
        FeedbackDtos.RatingSummary summary = summary(sellerId);
        RatingUpdate update = new RatingUpdate(summary.average(), (int) summary.count());
        try {
            identityClient.updateSellerRating(sellerId, update);
        } catch (RuntimeException ignored) {
        }
        try {
            catalogClient.updateSellerRating(sellerId, update);
        } catch (RuntimeException ignored) {
        }
    }

    private static void apply(FeedbackQuestion question, FeedbackDtos.QuestionRequest request) {
        question.setQuestionText(request.questionText().trim());
        question.setQuestionType(request.questionType());
        question.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        question.setMandatory(Boolean.TRUE.equals(request.mandatory()));
        question.setActive(request.active() == null || request.active());
    }
}
