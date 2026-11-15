package com.sidbi.bds.procurement.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.dto.QueryDtos;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.service.DocumentService;
import com.sidbi.bds.procurement.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Query Resolution Tracker", description = "Queries between sellers and purchasers, escalations and grievances")
public class QueryController {

    private final QueryService queryService;
    private final DocumentService documentService;

    @GetMapping("/queries")
    @Operation(summary = "List queries where you are a party")
    public ApiResponse<PageResponse<QueryDtos.TicketSummary>> list(@RequestParam(required = false) ProcEnums.TicketStatus status,
                                                                   @RequestParam(required = false) ProcEnums.ContextType contextType,
                                                                   @RequestParam(required = false) ProcEnums.TicketType ticketType,
                                                                   @RequestParam(required = false) String q,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(queryService.list(new QueryService.TicketFilter(status, contextType, ticketType, null, q), page, size));
    }

    @PostMapping("/queries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Raise a query on an RFQ, quotation, work order or engagement")
    public ApiResponse<QueryDtos.TicketDetail> create(@Valid @RequestBody QueryDtos.CreateTicketRequest request) {
        QueryDtos.TicketDetail detail = queryService.create(request);
        return ApiResponse.created("Query " + detail.summary().ticketNumber() + " raised", detail);
    }

    @GetMapping("/queries/{ticketId}")
    @Operation(summary = "Query detail with conversation")
    public ApiResponse<QueryDtos.TicketDetail> detail(@PathVariable Long ticketId) {
        return ApiResponse.ok(queryService.detail(ticketId));
    }

    @PostMapping("/queries/{ticketId}/messages")
    @Operation(summary = "Reply to a query")
    public ApiResponse<QueryDtos.TicketDetail> reply(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.MessageRequest request) {
        return ApiResponse.ok("Reply sent", queryService.reply(ticketId, request.body()));
    }

    @PostMapping("/queries/{ticketId}/resolve")
    @Operation(summary = "Mark a query as resolved")
    public ApiResponse<QueryDtos.TicketDetail> resolve(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.StatusRequest request) {
        return ApiResponse.ok("Query resolved", queryService.resolve(ticketId, request.remarks()));
    }

    @PostMapping("/queries/{ticketId}/reopen")
    @Operation(summary = "Reopen a resolved query")
    public ApiResponse<QueryDtos.TicketDetail> reopen(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.StatusRequest request) {
        return ApiResponse.ok("Query reopened", queryService.reopen(ticketId, request.remarks()));
    }

    @PostMapping("/queries/{ticketId}/escalate")
    @Operation(summary = "Escalate a query to platform support")
    public ApiResponse<QueryDtos.TicketDetail> escalate(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.StatusRequest request) {
        return ApiResponse.ok("Query escalated", queryService.escalate(ticketId, request.remarks()));
    }

    @PostMapping("/queries/{ticketId}/close")
    @Operation(summary = "Close a resolved query")
    public ApiResponse<QueryDtos.TicketDetail> close(@PathVariable Long ticketId, @Valid @RequestBody QueryDtos.StatusRequest request) {
        return ApiResponse.ok("Query closed", queryService.close(ticketId, request.remarks()));
    }

    @PostMapping(value = "/queries/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a file to a query")
    public ApiResponse<RfqDtos.DocumentResponse> attach(@PathVariable Long ticketId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.created("Attachment uploaded", queryService.addAttachment(ticketId, file));
    }

    @PostMapping("/grievances")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Raise a grievance with platform administration")
    public ApiResponse<QueryDtos.TicketDetail> grievance(@Valid @RequestBody QueryDtos.GrievanceRequest request) {
        QueryDtos.TicketDetail detail = queryService.grievance(request);
        return ApiResponse.created("Grievance " + detail.summary().ticketNumber() + " registered", detail);
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Download an RFQ, quotation, work order, engagement or query document")
    public ResponseEntity<Resource> download(@PathVariable Long documentId) {
        return documentService.download(documentId);
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Remove a document you uploaded")
    public ApiResponse<Void> delete(@PathVariable Long documentId) {
        documentService.delete(documentId);
        return ApiResponse.message("Document removed");
    }
}
