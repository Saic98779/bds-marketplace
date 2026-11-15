package com.sidbi.bds.procurement.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.api.RemarksRequest;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.dto.DashboardDtos;
import com.sidbi.bds.procurement.dto.EngagementDtos;
import com.sidbi.bds.procurement.dto.FeedbackDtos;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.service.DashboardService;
import com.sidbi.bds.procurement.service.EngagementService;
import com.sidbi.bds.procurement.service.FeedbackService;
import com.sidbi.bds.procurement.service.QuotationService;
import com.sidbi.bds.procurement.service.RfqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
@Tag(name = "Seller - Procurement", description = "RFQ inbox, quotations, work orders, engagements and reviews")
public class SellerProcurementController {

    private final RfqService rfqService;
    private final QuotationService quotationService;
    private final EngagementService engagementService;
    private final FeedbackService feedbackService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Seller dashboard")
    public ApiResponse<DashboardDtos.SellerDashboard> dashboard() {
        return ApiResponse.ok(dashboardService.seller(CurrentUser.subjectId()));
    }

    @GetMapping("/rfqs")
    @Operation(summary = "RFQ inbox")
    public ApiResponse<PageResponse<RfqDtos.RfqSummary>> inbox(@RequestParam(required = false) ProcEnums.RfqStatus status,
                                                               @RequestParam(required = false) ProcEnums.InvitationStatus invitationStatus,
                                                               @RequestParam(required = false) String q,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(rfqService.sellerInbox(status, invitationStatus, q, page, size));
    }

    @GetMapping("/rfqs/{rfqId}")
    @Operation(summary = "View an RFQ shared with you")
    public ApiResponse<RfqDtos.RfqDetail> rfq(@PathVariable Long rfqId) {
        return ApiResponse.ok(rfqService.sellerDetail(rfqId));
    }

    @PostMapping("/rfqs/{rfqId}/decline")
    @Operation(summary = "Decline to quote")
    public ApiResponse<RfqDtos.RfqDetail> decline(@PathVariable Long rfqId, @Valid @RequestBody RfqDtos.DeclineRequest request) {
        return ApiResponse.ok("RFQ declined", rfqService.decline(rfqId, request.reason()));
    }

    @PostMapping("/rfqs/{rfqId}/quotations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quotation as draft or submit")
    public ApiResponse<QuotationDtos.QuotationResponse> createQuotation(@PathVariable Long rfqId,
                                                                        @Valid @RequestBody QuotationDtos.QuotationRequest request) {
        QuotationDtos.QuotationResponse response = quotationService.create(rfqId, request);
        return ApiResponse.created(Boolean.TRUE.equals(request.submit()) ? "Quotation " + response.quotationNumber() + " submitted"
                : "Quotation saved as draft", response);
    }

    @GetMapping("/quotations")
    @Operation(summary = "List own quotations")
    public ApiResponse<PageResponse<QuotationDtos.QuotationResponse>> quotations(@RequestParam(required = false) ProcEnums.QuotationStatus status,
                                                                                 @RequestParam(required = false) String q,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(quotationService.sellerList(status, q, page, size));
    }

    @GetMapping("/quotations/{quotationId}")
    @Operation(summary = "Get own quotation")
    public ApiResponse<QuotationDtos.QuotationResponse> quotation(@PathVariable Long quotationId) {
        return ApiResponse.ok(quotationService.sellerDetail(quotationId));
    }

    @PutMapping("/quotations/{quotationId}")
    @Operation(summary = "Edit a draft or submit a controlled revision")
    public ApiResponse<QuotationDtos.QuotationResponse> update(@PathVariable Long quotationId, @Valid @RequestBody QuotationDtos.QuotationRequest request) {
        return ApiResponse.ok("Quotation updated", quotationService.update(quotationId, request));
    }

    @PostMapping("/quotations/{quotationId}/submit")
    @Operation(summary = "Submit a draft quotation")
    public ApiResponse<QuotationDtos.QuotationResponse> submit(@PathVariable Long quotationId) {
        return ApiResponse.ok("Quotation submitted", quotationService.submit(quotationId));
    }

    @PostMapping("/quotations/{quotationId}/withdraw")
    @Operation(summary = "Withdraw a quotation")
    public ApiResponse<QuotationDtos.QuotationResponse> withdraw(@PathVariable Long quotationId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Quotation withdrawn", quotationService.withdraw(quotationId, request.remarks()));
    }

    @GetMapping("/quotations/{quotationId}/versions")
    @Operation(summary = "Version history")
    public ApiResponse<List<QuotationDtos.VersionResponse>> versions(@PathVariable Long quotationId) {
        return ApiResponse.ok(quotationService.versions(quotationId));
    }

    @GetMapping("/quotations/{quotationId}/pdf")
    @Operation(summary = "Download quotation PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long quotationId) {
        return quotationService.pdf(quotationId);
    }

    @PostMapping(value = "/quotations/{quotationId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach technical or financial documents")
    public ApiResponse<RfqDtos.DocumentResponse> attach(@PathVariable Long quotationId,
                                                        @RequestParam(value = "category", required = false) String category,
                                                        @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Attachment uploaded", quotationService.addAttachment(quotationId, category, file));
    }

    @GetMapping("/work-orders")
    @Operation(summary = "Work orders")
    public ApiResponse<PageResponse<EngagementDtos.WorkOrderResponse>> workOrders(@RequestParam(required = false) ProcEnums.WorkOrderStatus status,
                                                                                  @RequestParam(defaultValue = "0") int page,
                                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(engagementService.workOrders(status, page, size));
    }

    @PostMapping("/work-orders/{workOrderId}/acknowledge")
    @Operation(summary = "Acknowledge an issued work order")
    public ApiResponse<EngagementDtos.WorkOrderResponse> acknowledge(@PathVariable Long workOrderId) {
        return ApiResponse.ok("Work order acknowledged", engagementService.acknowledgeWorkOrder(workOrderId));
    }

    @GetMapping("/engagements")
    @Operation(summary = "List engagements")
    public ApiResponse<PageResponse<EngagementDtos.EngagementSummary>> engagements(@RequestParam(required = false) ProcEnums.EngagementStatus status,
                                                                                   @RequestParam(required = false) String q,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(engagementService.list(status, q, page, size));
    }

    @GetMapping("/engagements/{engagementId}")
    @Operation(summary = "Engagement detail")
    public ApiResponse<EngagementDtos.EngagementDetail> engagement(@PathVariable Long engagementId) {
        return ApiResponse.ok(engagementService.detail(engagementId));
    }

    @PostMapping("/engagements/{engagementId}/work-order-request")
    @Operation(summary = "Request a work order from the purchaser")
    public ApiResponse<EngagementDtos.WorkOrderResponse> requestWorkOrder(@PathVariable Long engagementId,
                                                                          @Valid @RequestBody EngagementDtos.WorkOrderRequestRequest request) {
        return ApiResponse.ok("Work order requested", engagementService.requestWorkOrder(engagementId, request.remarks()));
    }

    @PostMapping("/engagements/{engagementId}/start")
    @Operation(summary = "Start work after work order is issued")
    public ApiResponse<EngagementDtos.EngagementDetail> start(@PathVariable Long engagementId) {
        return ApiResponse.ok("Engagement started", engagementService.start(engagementId));
    }

    @PostMapping("/engagements/{engagementId}/progress")
    @Operation(summary = "Post a progress update")
    public ApiResponse<EngagementDtos.EngagementDetail> progress(@PathVariable Long engagementId, @Valid @RequestBody EngagementDtos.ProgressRequest request) {
        return ApiResponse.ok("Progress updated", engagementService.progress(engagementId, request));
    }

    @PostMapping("/engagements/{engagementId}/deliver")
    @Operation(summary = "Mark deliverables as submitted")
    public ApiResponse<EngagementDtos.EngagementDetail> deliver(@PathVariable Long engagementId, @Valid @RequestBody EngagementDtos.DeliveryRequest request) {
        return ApiResponse.ok("Marked as delivered. Awaiting purchaser confirmation.", engagementService.deliver(engagementId, request));
    }

    @PostMapping("/engagements/{engagementId}/dispute")
    @Operation(summary = "Raise a dispute")
    public ApiResponse<EngagementDtos.EngagementDetail> dispute(@PathVariable Long engagementId, @Valid @RequestBody EngagementDtos.ReasonRequest request) {
        return ApiResponse.ok("Dispute raised and escalated", engagementService.dispute(engagementId, request.reason()));
    }

    @PostMapping("/engagements/{engagementId}/resume")
    @Operation(summary = "Resume a disputed engagement")
    public ApiResponse<EngagementDtos.EngagementDetail> resume(@PathVariable Long engagementId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Engagement resumed", engagementService.resume(engagementId, request.remarks()));
    }

    @PostMapping(value = "/engagements/{engagementId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload deliverables or engagement documents")
    public ApiResponse<RfqDtos.DocumentResponse> document(@PathVariable Long engagementId,
                                                          @RequestParam(value = "category", required = false) String category,
                                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Document uploaded", engagementService.addDocument(engagementId, category, file));
    }

    @GetMapping("/reviews")
    @Operation(summary = "Ratings and feedback received")
    public ApiResponse<PageResponse<FeedbackDtos.ReviewResponse>> reviews(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(feedbackService.sellerReviews(page, size));
    }

    @GetMapping("/reviews/summary")
    @Operation(summary = "Rating summary")
    public ApiResponse<FeedbackDtos.RatingSummary> summary() {
        return ApiResponse.ok(feedbackService.summary(CurrentUser.subjectId()));
    }
}
