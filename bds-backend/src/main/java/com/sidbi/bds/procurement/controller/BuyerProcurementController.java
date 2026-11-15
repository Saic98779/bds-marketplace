package com.sidbi.bds.procurement.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.api.RemarksRequest;
import com.sidbi.bds.common.api.RequiredRemarksRequest;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.dto.DashboardDtos;
import com.sidbi.bds.procurement.dto.EngagementDtos;
import com.sidbi.bds.procurement.dto.FeedbackDtos;
import com.sidbi.bds.procurement.dto.QuotationDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.service.ComparisonService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/buyer")
@RequiredArgsConstructor
@Tag(name = "Purchaser - Procurement", description = "RFQs, quotation evaluation, provider selection, work orders, engagements and feedback")
public class BuyerProcurementController {

    private final RfqService rfqService;
    private final QuotationService quotationService;
    private final ComparisonService comparisonService;
    private final EngagementService engagementService;
    private final FeedbackService feedbackService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Purchaser dashboard")
    public ApiResponse<DashboardDtos.BuyerDashboard> dashboard() {
        return ApiResponse.ok(dashboardService.buyer(CurrentUser.subjectId()));
    }

    @GetMapping("/rfqs")
    @Operation(summary = "List own RFQs")
    public ApiResponse<PageResponse<RfqDtos.RfqSummary>> rfqs(@RequestParam(required = false) ProcEnums.RfqStatus status,
                                                              @RequestParam(required = false) String q,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(rfqService.buyerList(status, q, page, size));
    }

    @PostMapping("/rfqs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an RFQ as draft or publish to providers")
    public ApiResponse<RfqDtos.RfqDetail> createRfq(@Valid @RequestBody RfqDtos.RfqRequest request) {
        RfqDtos.RfqDetail detail = rfqService.create(request);
        return ApiResponse.created(Boolean.TRUE.equals(request.publish())
                ? "RFQ " + detail.summary().rfqNumber() + " submitted" : "RFQ saved as draft", detail);
    }

    @GetMapping("/rfqs/{rfqId}")
    @Operation(summary = "Get own RFQ")
    public ApiResponse<RfqDtos.RfqDetail> rfq(@PathVariable Long rfqId) {
        return ApiResponse.ok(rfqService.buyerDetail(rfqId));
    }

    @PutMapping("/rfqs/{rfqId}")
    @Operation(summary = "Update a draft RFQ")
    public ApiResponse<RfqDtos.RfqDetail> updateRfq(@PathVariable Long rfqId, @Valid @RequestBody RfqDtos.RfqRequest request) {
        return ApiResponse.ok("RFQ updated", rfqService.update(rfqId, request));
    }

    @PostMapping("/rfqs/{rfqId}/publish")
    @Operation(summary = "Publish a draft RFQ")
    public ApiResponse<RfqDtos.RfqDetail> publish(@PathVariable Long rfqId) {
        return ApiResponse.ok("RFQ submitted", rfqService.publish(rfqId));
    }

    @PostMapping("/rfqs/{rfqId}/invite")
    @Operation(summary = "Invite additional verified providers")
    public ApiResponse<RfqDtos.RfqDetail> invite(@PathVariable Long rfqId, @Valid @RequestBody RfqDtos.InviteRequest request) {
        return ApiResponse.ok("Providers invited", rfqService.invite(rfqId, request.sellerIds()));
    }

    @PostMapping("/rfqs/{rfqId}/extend-deadline")
    @Operation(summary = "Extend the submission deadline")
    public ApiResponse<RfqDtos.RfqDetail> extend(@PathVariable Long rfqId, @Valid @RequestBody RfqDtos.ExtendDeadlineRequest request) {
        return ApiResponse.ok("Deadline extended", rfqService.extendDeadline(rfqId, request));
    }

    @PostMapping("/rfqs/{rfqId}/cancel")
    @Operation(summary = "Cancel an RFQ")
    public ApiResponse<RfqDtos.RfqDetail> cancel(@PathVariable Long rfqId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("RFQ cancelled", rfqService.cancel(rfqId, request.remarks()));
    }

    @PostMapping("/rfqs/{rfqId}/close")
    @Operation(summary = "Close an RFQ")
    public ApiResponse<RfqDtos.RfqDetail> close(@PathVariable Long rfqId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("RFQ closed", rfqService.close(rfqId, request.remarks()));
    }

    @PostMapping(value = "/rfqs/{rfqId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach supporting documents to an RFQ")
    public ApiResponse<RfqDtos.DocumentResponse> rfqAttachment(@PathVariable Long rfqId,
                                                               @RequestParam(value = "category", required = false) String category,
                                                               @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Attachment uploaded", rfqService.addAttachment(rfqId, category, file));
    }

    @GetMapping("/recommended-sellers")
    @Operation(summary = "Verified providers recommended for a category")
    public ApiResponse<List<RfqDtos.RecommendedSeller>> recommended(@RequestParam Long categoryId,
                                                                    @RequestParam(required = false) Long subcategoryId) {
        return ApiResponse.ok(rfqService.recommended(categoryId, subcategoryId));
    }

    @GetMapping("/rfqs/{rfqId}/quotations")
    @Operation(summary = "Quotations received for an RFQ")
    public ApiResponse<List<QuotationDtos.QuotationResponse>> rfqQuotations(@PathVariable Long rfqId) {
        return ApiResponse.ok(quotationService.forRfq(rfqId));
    }

    @GetMapping("/rfqs/{rfqId}/comparison")
    @Operation(summary = "Side-by-side technical and financial comparison")
    public ApiResponse<QuotationDtos.ComparisonResponse> comparison(@PathVariable Long rfqId) {
        return ApiResponse.ok(comparisonService.get(rfqId));
    }

    @PostMapping("/rfqs/{rfqId}/comparison")
    @Operation(summary = "Save evaluation criteria and weighted scores")
    public ApiResponse<QuotationDtos.ComparisonResponse> saveComparison(@PathVariable Long rfqId,
                                                                        @Valid @RequestBody QuotationDtos.ComparisonRequest request) {
        return ApiResponse.ok("Evaluation saved", comparisonService.save(rfqId, request));
    }

    @GetMapping("/rfqs/{rfqId}/comparison/export")
    @Operation(summary = "Download comparison report")
    public ResponseEntity<byte[]> exportComparison(@PathVariable Long rfqId, @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return comparisonService.export(rfqId, format);
    }

    @GetMapping("/quotations")
    @Operation(summary = "All quotations received")
    public ApiResponse<PageResponse<QuotationDtos.QuotationResponse>> quotations(@RequestParam(required = false) ProcEnums.QuotationStatus status,
                                                                                 @RequestParam(required = false) String q,
                                                                                 @RequestParam(required = false) Long rfqId,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(quotationService.buyerList(status, q, rfqId, page, size));
    }

    @GetMapping("/quotations/{quotationId}")
    @Operation(summary = "View a quotation")
    public ApiResponse<QuotationDtos.QuotationResponse> quotation(@PathVariable Long quotationId) {
        return ApiResponse.ok(quotationService.buyerDetail(quotationId));
    }

    @GetMapping("/quotations/{quotationId}/versions")
    @Operation(summary = "Version history of a quotation")
    public ApiResponse<List<QuotationDtos.VersionResponse>> versions(@PathVariable Long quotationId) {
        return ApiResponse.ok(quotationService.versions(quotationId));
    }

    @GetMapping("/quotations/{quotationId}/pdf")
    @Operation(summary = "Download quotation as PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long quotationId) {
        return quotationService.pdf(quotationId);
    }

    @PostMapping("/quotations/{quotationId}/shortlist")
    @Operation(summary = "Shortlist or remove from shortlist")
    public ApiResponse<QuotationDtos.QuotationResponse> shortlist(@PathVariable Long quotationId, @RequestBody Map<String, Boolean> body) {
        return ApiResponse.ok("Shortlist updated", quotationService.shortlist(quotationId, !Boolean.FALSE.equals(body.get("shortlisted"))));
    }

    @PostMapping("/quotations/{quotationId}/reject")
    @Operation(summary = "Reject a quotation")
    public ApiResponse<QuotationDtos.QuotationResponse> reject(@PathVariable Long quotationId, @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Quotation rejected", quotationService.reject(quotationId, request.remarks()));
    }

    @PostMapping("/quotations/{quotationId}/request-clarification")
    @Operation(summary = "Request clarification from the provider")
    public ApiResponse<QuotationDtos.QuotationResponse> clarification(@PathVariable Long quotationId,
                                                                      @Valid @RequestBody QuotationDtos.ClarificationRequest request) {
        return ApiResponse.ok("Clarification requested", quotationService.requestClarification(quotationId, request));
    }

    @PostMapping("/quotations/{quotationId}/accept")
    @Operation(summary = "Accept a quotation and select the provider")
    public ApiResponse<EngagementDtos.EngagementDetail> accept(@PathVariable Long quotationId, @Valid @RequestBody QuotationDtos.AcceptRequest request) {
        return ApiResponse.ok("Provider selected. Engagement created.", quotationService.accept(quotationId, request));
    }

    @GetMapping("/work-orders")
    @Operation(summary = "Work orders and requests")
    public ApiResponse<PageResponse<EngagementDtos.WorkOrderResponse>> workOrders(@RequestParam(required = false) ProcEnums.WorkOrderStatus status,
                                                                                  @RequestParam(defaultValue = "0") int page,
                                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(engagementService.workOrders(status, page, size));
    }

    @PostMapping(value = "/work-orders/{workOrderId}/issue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Issue a requested work order")
    public ApiResponse<EngagementDtos.WorkOrderResponse> issue(@PathVariable Long workOrderId,
                                                               @Valid @RequestPart("data") EngagementDtos.IssueWorkOrderRequest request,
                                                               @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok("Work order issued", engagementService.issueWorkOrder(workOrderId, request, file));
    }

    @PostMapping(value = "/work-orders/{workOrderId}/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Issue a requested work order without attachment")
    public ApiResponse<EngagementDtos.WorkOrderResponse> issueJson(@PathVariable Long workOrderId,
                                                                   @Valid @RequestBody EngagementDtos.IssueWorkOrderRequest request) {
        return ApiResponse.ok("Work order issued", engagementService.issueWorkOrder(workOrderId, request, null));
    }

    @PostMapping("/work-orders/{workOrderId}/decline")
    @Operation(summary = "Decline a work order request")
    public ApiResponse<EngagementDtos.WorkOrderResponse> declineWorkOrder(@PathVariable Long workOrderId,
                                                                          @Valid @RequestBody RequiredRemarksRequest request) {
        return ApiResponse.ok("Work order request declined", engagementService.declineWorkOrder(workOrderId, request.remarks()));
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

    @PostMapping(value = "/engagements/{engagementId}/work-order", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Issue a work order directly for an engagement")
    public ApiResponse<EngagementDtos.WorkOrderResponse> issueDirect(@PathVariable Long engagementId,
                                                                     @Valid @RequestPart("data") EngagementDtos.IssueWorkOrderRequest request,
                                                                     @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok("Work order issued", engagementService.issueDirect(engagementId, request, file));
    }

    @PostMapping(value = "/engagements/{engagementId}/work-order", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Issue a work order directly without attachment")
    public ApiResponse<EngagementDtos.WorkOrderResponse> issueDirectJson(@PathVariable Long engagementId,
                                                                         @Valid @RequestBody EngagementDtos.IssueWorkOrderRequest request) {
        return ApiResponse.ok("Work order issued", engagementService.issueDirect(engagementId, request, null));
    }

    @PostMapping("/engagements/{engagementId}/confirm-delivery")
    @Operation(summary = "Confirm whether the work has been delivered")
    public ApiResponse<EngagementDtos.EngagementDetail> confirmDelivery(@PathVariable Long engagementId,
                                                                        @Valid @RequestBody EngagementDtos.ConfirmDeliveryRequest request) {
        return ApiResponse.ok(Boolean.TRUE.equals(request.delivered()) ? "Delivery confirmed. Please share your feedback."
                : "Delivery not accepted. A query has been raised with the provider.", engagementService.confirmDelivery(engagementId, request));
    }

    @PostMapping("/engagements/{engagementId}/dispute")
    @Operation(summary = "Raise a dispute on the engagement")
    public ApiResponse<EngagementDtos.EngagementDetail> dispute(@PathVariable Long engagementId, @Valid @RequestBody EngagementDtos.ReasonRequest request) {
        return ApiResponse.ok("Dispute raised and escalated", engagementService.dispute(engagementId, request.reason()));
    }

    @PostMapping("/engagements/{engagementId}/resume")
    @Operation(summary = "Resume a disputed engagement")
    public ApiResponse<EngagementDtos.EngagementDetail> resume(@PathVariable Long engagementId, @Valid @RequestBody RemarksRequest request) {
        return ApiResponse.ok("Engagement resumed", engagementService.resume(engagementId, request.remarks()));
    }

    @PostMapping("/engagements/{engagementId}/cancel")
    @Operation(summary = "Cancel the engagement")
    public ApiResponse<EngagementDtos.EngagementDetail> cancelEngagement(@PathVariable Long engagementId, @Valid @RequestBody EngagementDtos.ReasonRequest request) {
        return ApiResponse.ok("Engagement cancelled", engagementService.cancel(engagementId, request.reason()));
    }

    @PostMapping(value = "/engagements/{engagementId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an engagement document")
    public ApiResponse<RfqDtos.DocumentResponse> engagementDocument(@PathVariable Long engagementId,
                                                                    @RequestParam(value = "category", required = false) String category,
                                                                    @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Document uploaded", engagementService.addDocument(engagementId, category, file));
    }

    @GetMapping("/feedback/questions")
    @Operation(summary = "Active feedback questions")
    public ApiResponse<List<FeedbackDtos.QuestionResponse>> questions() {
        return ApiResponse.ok(feedbackService.activeQuestions());
    }

    @PostMapping("/engagements/{engagementId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit star rating and feedback after delivery")
    public ApiResponse<FeedbackDtos.ReviewResponse> feedback(@PathVariable Long engagementId, @Valid @RequestBody FeedbackDtos.FeedbackRequest request) {
        return ApiResponse.created("Thank you for your feedback", feedbackService.submit(engagementId, request));
    }
}
