package com.sidbi.bds.procurement.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.dto.DashboardDtos;
import com.sidbi.bds.procurement.dto.EngagementDtos;
import com.sidbi.bds.procurement.dto.FeedbackDtos;
import com.sidbi.bds.procurement.dto.QueryDtos;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.service.ComparisonService;
import com.sidbi.bds.procurement.service.DashboardService;
import com.sidbi.bds.procurement.service.EngagementService;
import com.sidbi.bds.procurement.service.FeedbackService;
import com.sidbi.bds.procurement.service.QueryService;
import com.sidbi.bds.procurement.service.QuotationService;
import com.sidbi.bds.procurement.service.RfqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Procurement Monitoring", description = "RFQ, quotation and engagement monitoring, grievances, feedback and MIS")
public class AdminProcurementController {

    private final RfqService rfqService;
    private final QuotationService quotationService;
    private final ComparisonService comparisonService;
    private final EngagementService engagementService;
    private final QueryService queryService;
    private final FeedbackService feedbackService;
    private final DashboardService dashboardService;

    @GetMapping("/procurement/stats")
    @Operation(summary = "Procurement KPIs and trends")
    public ApiResponse<DashboardDtos.ProcurementStats> stats() {
        return ApiResponse.ok(dashboardService.stats());
    }

    @GetMapping("/rfqs")
    @Operation(summary = "Monitor RFQs")
    public ApiResponse<PageResponse<RfqDtos.RfqSummary>> rfqs(@RequestParam(required = false) ProcEnums.RfqStatus status,
                                                              @RequestParam(required = false) String q,
                                                              @RequestParam(required = false) Long categoryId,
                                                              @RequestParam(required = false) Long buyerId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(rfqService.adminList(new RfqService.RfqFilter(status, q, categoryId, buyerId), page, size));
    }

    @GetMapping("/rfqs/{rfqId}")
    @Operation(summary = "RFQ detail with invited providers")
    public ApiResponse<RfqDtos.RfqDetail> rfq(@PathVariable Long rfqId) {
        return ApiResponse.ok(rfqService.adminDetail(rfqId));
    }

    @PostMapping("/rfqs/{rfqId}/invite")
    @Operation(summary = "Administrator-assisted provider matching")
    public ApiResponse<RfqDtos.RfqDetail> invite(@PathVariable Long rfqId, @Valid @RequestBody RfqDtos.InviteRequest request) {
        return ApiResponse.ok("Providers matched", rfqService.adminInvite(rfqId, request.sellerIds()));
    }

    @GetMapping("/rfqs/{rfqId}/recommended-sellers")
    @Operation(summary = "Recommended providers for an RFQ")
    public ApiResponse<List<RfqDtos.RecommendedSeller>> recommended(@PathVariable Long rfqId) {
        RfqDtos.RfqDetail rfq = rfqService.adminDetail(rfqId);
        return ApiResponse.ok(rfqService.recommended(rfq.summary().categoryId(), null));
    }

    @GetMapping("/rfqs/{rfqId}/comparison")
    @Operation(summary = "View the comparison (read-only)")
    public ApiResponse<QuotationDtos.ComparisonResponse> comparison(@PathVariable Long rfqId) {
        return ApiResponse.ok(comparisonService.get(rfqId));
    }

    @GetMapping("/quotations")
    @Operation(summary = "Monitor quotation submissions")
    public ApiResponse<PageResponse<QuotationDtos.QuotationResponse>> quotations(@RequestParam(required = false) ProcEnums.QuotationStatus status,
                                                                                 @RequestParam(required = false) String q,
                                                                                 @RequestParam(required = false) Long rfqId,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(quotationService.adminList(new QuotationService.QuotationFilter(status, q, null, null, rfqId), page, size));
    }

    @GetMapping("/quotations/{quotationId}")
    @Operation(summary = "View a quotation (read-only)")
    public ApiResponse<QuotationDtos.QuotationResponse> quotation(@PathVariable Long quotationId) {
        return ApiResponse.ok(quotationService.adminDetail(quotationId));
    }

    @GetMapping("/engagements")
    @Operation(summary = "Monitor engagements")
    public ApiResponse<PageResponse<EngagementDtos.EngagementSummary>> engagements(@RequestParam(required = false) ProcEnums.EngagementStatus status,
                                                                                   @RequestParam(required = false) String q,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(engagementService.list(status, q, page, size));
    }

    @GetMapping("/engagements/{engagementId}")
    @Operation(summary = "Engagement detail (read-only)")
    public ApiResponse<EngagementDtos.EngagementDetail> engagement(@PathVariable Long engagementId) {
        return ApiResponse.ok(engagementService.detail(engagementId));
    }

    @GetMapping("/queries")
    @Operation(summary = "Escalated queries and grievances")
    public ApiResponse<PageResponse<QueryDtos.TicketSummary>> queries(@RequestParam(required = false) ProcEnums.TicketStatus status,
                                                                      @RequestParam(required = false) ProcEnums.TicketType ticketType,
                                                                      @RequestParam(required = false) Boolean escalated,
                                                                      @RequestParam(required = false) String q,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(queryService.list(new QueryService.TicketFilter(status, null, ticketType, escalated, q), page, size));
    }

    @GetMapping("/queries/{ticketId}")
    @Operation(summary = "Query or grievance detail")
    public ApiResponse<QueryDtos.TicketDetail> query(@PathVariable Long ticketId) {
        return ApiResponse.ok(queryService.detail(ticketId));
    }

    @PostMapping("/queries/{ticketId}/messages")
    @Operation(summary = "Respond to an escalated query or grievance")
    public ApiResponse<QueryDtos.TicketDetail> reply(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.MessageRequest request) {
        return ApiResponse.ok("Response sent", queryService.reply(ticketId, request.body()));
    }

    @PostMapping("/queries/{ticketId}/resolve")
    @Operation(summary = "Resolve an escalated query or grievance")
    public ApiResponse<QueryDtos.TicketDetail> resolve(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.StatusRequest request) {
        return ApiResponse.ok("Resolved", queryService.resolve(ticketId, request.remarks()));
    }

    @PostMapping("/queries/{ticketId}/close")
    @Operation(summary = "Close an escalated query or grievance")
    public ApiResponse<QueryDtos.TicketDetail> close(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.StatusRequest request) {
        return ApiResponse.ok("Closed", queryService.close(ticketId, request.remarks()));
    }

    @GetMapping("/feedback-questions")
    @Operation(summary = "All feedback questions")
    public ApiResponse<List<FeedbackDtos.QuestionResponse>> questions() {
        return ApiResponse.ok(feedbackService.allQuestions());
    }

    @PostMapping("/feedback-questions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a feedback question")
    public ApiResponse<FeedbackDtos.QuestionResponse> createQuestion(@Valid @RequestBody FeedbackDtos.QuestionRequest request) {
        return ApiResponse.created("Question created", feedbackService.createQuestion(request));
    }

    @PutMapping("/feedback-questions/{questionId}")
    @Operation(summary = "Update a feedback question")
    public ApiResponse<FeedbackDtos.QuestionResponse> updateQuestion(@PathVariable Long questionId, @Valid @RequestBody FeedbackDtos.QuestionRequest request) {
        return ApiResponse.ok("Question updated", feedbackService.updateQuestion(questionId, request));
    }

    @GetMapping("/reviews")
    @Operation(summary = "All reviews")
    public ApiResponse<PageResponse<FeedbackDtos.ReviewResponse>> reviews(@RequestParam(required = false) Long sellerId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(feedbackService.adminReviews(sellerId, page, size));
    }

    @PutMapping("/reviews/{reviewId}/visibility")
    @Operation(summary = "Publish or hide a review")
    public ApiResponse<FeedbackDtos.ReviewResponse> visibility(@PathVariable Long reviewId, @Valid @RequestBody FeedbackDtos.VisibilityRequest request) {
        return ApiResponse.ok("Review visibility updated", feedbackService.setVisibility(reviewId, request));
    }

    @GetMapping("/procurement/reports/rfqs")
    @Operation(summary = "Export RFQ report")
    public ResponseEntity<byte[]> rfqReport(@RequestParam(required = false) ProcEnums.RfqStatus status,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return dashboardService.rfqReport(status, categoryId, format);
    }

    @GetMapping("/procurement/reports/quotations")
    @Operation(summary = "Export quotation report")
    public ResponseEntity<byte[]> quotationReport(@RequestParam(required = false) ProcEnums.QuotationStatus status,
                                                  @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return dashboardService.quotationReport(status, format);
    }

    @GetMapping("/procurement/reports/engagements")
    @Operation(summary = "Export engagement report")
    public ResponseEntity<byte[]> engagementReport(@RequestParam(required = false) ProcEnums.EngagementStatus status,
                                                   @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return dashboardService.engagementReport(status, format);
    }

    @GetMapping("/procurement/reports/grievances")
    @Operation(summary = "Export grievance report")
    public ResponseEntity<byte[]> grievanceReport(@RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return dashboardService.grievanceReport(format);
    }
}
