package com.sidbi.bds.auth.controller;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.dto.AdminDtos;
import com.sidbi.bds.auth.service.IdentityReportService;
import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.report.ReportFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/identity")
@RequiredArgsConstructor
@Tag(name = "Admin - Identity MIS", description = "Registration statistics and exports")
public class AdminIdentityController {

    private final IdentityReportService reportService;

    @GetMapping("/stats")
    @Operation(summary = "Seller and purchaser registration statistics")
    public ApiResponse<AdminDtos.IdentityStats> stats() {
        return ApiResponse.ok(reportService.stats());
    }

    @GetMapping("/reports/sellers")
    @Operation(summary = "Export seller registrations")
    public ResponseEntity<byte[]> sellers(@RequestParam(required = false) Enums.UserStatus status,
                                          @RequestParam(required = false) String state,
                                          @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return reportService.sellersReport(status, state, format);
    }

    @GetMapping("/reports/buyers")
    @Operation(summary = "Export purchaser registrations")
    public ResponseEntity<byte[]> buyers(@RequestParam(required = false) Enums.UserStatus status,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return reportService.buyersReport(status, state, format);
    }
}
