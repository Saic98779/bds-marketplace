package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcEnums.QuotationStatus;
import com.sidbi.bds.procurement.domain.ProcEnums.RfqStatus;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.RfqInvitation;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import com.sidbi.bds.procurement.repository.RfqInvitationRepository;
import com.sidbi.bds.procurement.repository.RfqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcurementScheduler {

    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final RfqRepository rfqRepository;
    private final RfqInvitationRepository invitationRepository;
    private final QuotationRepository quotationRepository;
    private final NotificationClient notificationClient;

    @Value("${bds.procurement.reminder-hours:24}")
    private int reminderHours;

    @Value("${bds.procurement.expiry-days-after-deadline:60}")
    private int expiryDays;

    @Scheduled(fixedDelayString = "${bds.procurement.scheduler-interval-ms:300000}", initialDelay = 60_000)
    @Transactional
    public void processDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        for (Rfq rfq : rfqRepository.findByStatusInAndSubmissionDeadlineBefore(RfqStatus.OPEN_FOR_QUOTES, now)) {
            long quotations = quotationRepository.countByRfqIdAndStatusIn(rfq.getId(), QuotationStatus.VISIBLE_TO_BUYER);
            rfq.setStatus(quotations > 0 ? RfqStatus.UNDER_EVALUATION : RfqStatus.NO_RESPONSE);
            notificationClient.notify(rfq.getBuyerId(), quotations > 0 ? "RFQ_DEADLINE_CLOSED" : "RFQ_NO_RESPONSE", "RFQ",
                    "/buyer/rfqs/" + rfq.getId(), Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "count", String.valueOf(quotations)));
        }
        for (Rfq rfq : rfqRepository.findByStatusInAndSubmissionDeadlineBefore(EnumSet.of(RfqStatus.UNDER_EVALUATION),
                now.minusDays(expiryDays))) {
            rfq.setStatus(RfqStatus.EXPIRED);
            rfq.setStatusRemarks("No provider selected within " + expiryDays + " days of the deadline");
            for (Quotation q : quotationRepository.findByRfqIdAndStatusIn(rfq.getId(), QuotationStatus.LIVE)) {
                q.setStatus(QuotationStatus.EXPIRED);
            }
        }
        for (Quotation q : quotationRepository.findByStatusInAndValidUntilBefore(QuotationStatus.LIVE, LocalDate.now())) {
            q.setStatus(QuotationStatus.EXPIRED);
            q.setDecisionRemarks("Quotation validity lapsed");
            notificationClient.notify(q.getSellerId(), "QUOTATION_EXPIRED", "QUOTATION", "/seller/quotations/" + q.getId(),
                    Map.of("quotation", q.getQuotationNumber() == null ? "" : q.getQuotationNumber(), "rfq", q.getRfq().getRfqNumber()));
        }
    }

    @Scheduled(fixedDelayString = "${bds.procurement.scheduler-interval-ms:300000}", initialDelay = 90_000)
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (RfqInvitation invitation : invitationRepository.dueForReminder(
                EnumSet.of(ProcEnums.InvitationStatus.INVITED, ProcEnums.InvitationStatus.VIEWED), RfqStatus.OPEN_FOR_QUOTES,
                now, now.plusHours(reminderHours))) {
            Rfq rfq = invitation.getRfq();
            invitation.setReminderSent(true);
            notificationClient.notify(invitation.getSellerId(), "RFQ_DEADLINE_REMINDER", "RFQ", "/seller/rfqs/" + rfq.getId(),
                    Map.of("rfq", rfq.getRfqNumber(), "title", rfq.getTitle(), "deadline", rfq.getSubmissionDeadline().format(DEADLINE)));
        }
    }
}
