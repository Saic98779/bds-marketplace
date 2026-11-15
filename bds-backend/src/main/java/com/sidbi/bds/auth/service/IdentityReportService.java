package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.PurchaserProfile;
import com.sidbi.bds.auth.domain.SellerProfile;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.auth.dto.AdminDtos;
import com.sidbi.bds.auth.repository.PurchaserProfileRepository;
import com.sidbi.bds.auth.repository.SellerProfileRepository;
import com.sidbi.bds.auth.repository.UserRepository;
import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.report.ReportExporter;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.common.report.ReportTable;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import com.sidbi.bds.common.util.Csv;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IdentityReportService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PurchaserProfileRepository purchaserProfileRepository;
    private final ReportExporter exporter;
    private final NotificationClient notificationClient;

    @Transactional(readOnly = true)
    public AdminDtos.IdentityStats stats() {
        CurrentUser.require(Permission.ADMIN_DASHBOARD);
        Map<String, Long> sellers = new LinkedHashMap<>();
        Map<String, Long> buyers = new LinkedHashMap<>();
        for (Enums.UserStatus status : Enums.UserStatus.values()) {
            sellers.put(status.name(), 0L);
            buyers.put(status.name(), 0L);
        }
        for (Object[] row : userRepository.countByRoleAndStatus()) {
            Role role = (Role) row[0];
            String status = ((Enums.UserStatus) row[1]).name();
            long count = ((Number) row[2]).longValue();
            if (role == Role.SELLER) {
                sellers.put(status, count);
            } else if (role == Role.BUYER) {
                buyers.put(status, count);
            }
        }
        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(11);
        Map<YearMonth, long[]> trend = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            trend.put(start.plusMonths(i), new long[2]);
        }
        for (User u : userRepository.findRegisteredSince(Role.SELLER, start.atDay(1).atStartOfDay())) {
            long[] bucket = trend.get(YearMonth.from(u.getCreatedAt()));
            if (bucket != null) {
                bucket[0]++;
            }
        }
        for (User u : userRepository.findRegisteredSince(Role.BUYER, start.atDay(1).atStartOfDay())) {
            long[] bucket = trend.get(YearMonth.from(u.getCreatedAt()));
            if (bucket != null) {
                bucket[1]++;
            }
        }
        List<AdminDtos.TrendPoint> points = new ArrayList<>();
        trend.forEach((month, values) -> points.add(new AdminDtos.TrendPoint(month.format(MONTH), values[0], values[1])));
        long pendingSellers = sellers.get("PENDING_VERIFICATION") + sellers.get("INFO_REQUESTED");
        long pendingBuyers = buyers.get("PENDING_VERIFICATION") + buyers.get("INFO_REQUESTED");
        return new AdminDtos.IdentityStats(
                sellers.values().stream().mapToLong(Long::longValue).sum(),
                buyers.values().stream().mapToLong(Long::longValue).sum(),
                pendingSellers,
                pendingBuyers,
                sellers,
                buyers,
                points,
                counts(sellerProfileRepository.countByState(), 12),
                counts(sellerProfileRepository.countByProviderType(), 20),
                counts(purchaserProfileRepository.countByType(), 20),
                counts(purchaserProfileRepository.countByIndustry(), 12));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> sellersReport(Enums.UserStatus status, String state, ReportFormat format) {
        CurrentUser.require(Permission.REPORT_VIEW);
        ReportTable table = new ReportTable("Seller Registration Report", "Seller ID", "Organisation", "Contact", "Provider Type",
                "Constitution", "Categories", "State", "District", "Experience (yrs)", "Status", "Rating", "Registered On", "Verified On");
        for (SellerProfile p : sellerProfileRepository.findAll(Sort.by("organisationName"))) {
            User u = p.getUser();
            if ((status != null && u.getStatus() != status) || (state != null && !state.isBlank() && !state.equals(p.getState()))) {
                continue;
            }
            table.row(u.getReferenceNo(), p.getOrganisationName(), u.getFullName(), p.getProviderType(), p.getConstitution(),
                    String.join("; ", Csv.split(p.getCategoryNames())), p.getState(), p.getDistrict(), p.getYearsExperience(),
                    u.getStatus(), p.getAverageRating() == null ? "-" : String.format("%.1f", p.getAverageRating()),
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().format(DATE),
                    p.getVerifiedAt() == null ? "" : p.getVerifiedAt().format(DATE));
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "sellers", format.name());
        return exporter.export(table, format, "seller-report");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> buyersReport(Enums.UserStatus status, String state, ReportFormat format) {
        CurrentUser.require(Permission.REPORT_VIEW);
        ReportTable table = new ReportTable("Purchaser Registration Report", "Purchaser ID", "Organisation", "Contact", "Purchaser Type",
                "Industry", "State", "District", "Status", "Registered On");
        for (PurchaserProfile p : purchaserProfileRepository.findAll(Sort.by("organisationName"))) {
            User u = p.getUser();
            if ((status != null && u.getStatus() != status) || (state != null && !state.isBlank() && !state.equals(p.getState()))) {
                continue;
            }
            table.row(u.getReferenceNo(), p.getOrganisationName(), u.getFullName(), p.getPurchaserType(), p.getIndustrySector(),
                    p.getState(), p.getDistrict(), u.getStatus(), u.getCreatedAt() == null ? "" : u.getCreatedAt().format(DATE));
        }
        notificationClient.audit("REPORT_EXPORTED", "REPORT", "purchasers", format.name());
        return exporter.export(table, format, "purchaser-report");
    }

    private static List<AdminDtos.CountItem> counts(List<Object[]> rows, int limit) {
        return rows.stream()
                .limit(limit)
                .map(r -> new AdminDtos.CountItem(r[0] == null ? "Unspecified" : String.valueOf(r[0]), ((Number) r[1]).longValue()))
                .toList();
    }
}
