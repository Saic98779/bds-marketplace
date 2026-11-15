package com.sidbi.bds.procurement.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.QuotationItem;
import com.sidbi.bds.procurement.domain.Rfq;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class QuotationPdfService {

    private static final Color NAVY = new Color(11, 42, 66);
    private static final Color TEAL = new Color(11, 110, 142);
    private static final Color MUTED = new Color(74, 98, 117);
    private static final Color SHADE = new Color(241, 245, 248);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] render(Quotation q) {
        Rfq rfq = q.getRfq();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();
        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, NAVY);
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEAL);
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
        Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        document.add(new Paragraph("BDS Marketplace", brand));
        Paragraph title = new Paragraph("Technical & Financial Quotation", heading);
        title.setSpacingAfter(10);
        document.add(title);

        PdfPTable meta = new PdfPTable(4);
        meta.setWidthPercentage(100);
        cell(meta, "Quotation No.", label, SHADE);
        cell(meta, q.getQuotationNumber() + " (v" + Math.max(1, q.getVersionNo()) + ")", body, null);
        cell(meta, "Status", label, SHADE);
        cell(meta, q.getStatus().name().replace('_', ' '), body, null);
        cell(meta, "RFQ Reference", label, SHADE);
        cell(meta, rfq.getRfqNumber(), body, null);
        cell(meta, "Requirement", label, SHADE);
        cell(meta, rfq.getTitle(), body, null);
        cell(meta, "Service Provider", label, SHADE);
        cell(meta, nz(q.getSellerName()), body, null);
        cell(meta, "Purchaser", label, SHADE);
        cell(meta, nz(rfq.getBuyerName()), body, null);
        cell(meta, "Submitted On", label, SHADE);
        cell(meta, q.getSubmittedAt() == null ? "Draft" : q.getSubmittedAt().format(DATE_TIME), body, null);
        cell(meta, "Valid Until", label, SHADE);
        cell(meta, q.getValidUntil() == null ? "-" : q.getValidUntil().format(DATE), body, null);
        document.add(meta);

        section(document, "Technical Proposal", q.getTechnicalProposal(), heading, body);
        section(document, "Methodology", q.getMethodology(), heading, body);
        section(document, "Scope", q.isScopeAccepted() ? "Scope accepted as specified in the RFQ."
                : "Deviations: " + nz(q.getScopeDeviations()), heading, body);
        section(document, "Deliverables", q.getDeliverables(), heading, body);
        section(document, "Timeline", q.getTimelineDays() == null ? null : q.getTimelineDays() + " days", heading, body);
        section(document, "Team Composition", q.getTeamComposition(), heading, body);
        section(document, "Relevant Experience", q.getRelevantExperience(), heading, body);

        Paragraph financial = new Paragraph("Financial Proposal", heading);
        financial.setSpacingBefore(10);
        financial.setSpacingAfter(4);
        document.add(financial);
        if (!q.getItems().isEmpty()) {
            PdfPTable items = new PdfPTable(new float[]{5, 1.4f, 1.2f, 2, 2});
            items.setWidthPercentage(100);
            for (String h : new String[]{"Description", "Qty", "Unit", "Unit Price", "Amount"}) {
                cell(items, h, white, TEAL);
            }
            for (QuotationItem item : q.getItems()) {
                cell(items, item.getDescription(), body, null);
                cell(items, item.getQuantity().stripTrailingZeros().toPlainString(), body, null);
                cell(items, nz(item.getUnit()), body, null);
                money(items, item.getUnitPrice(), body);
                money(items, item.getAmount(), body);
            }
            document.add(items);
        }
        PdfPTable totals = new PdfPTable(new float[]{4, 2});
        totals.setWidthPercentage(60);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(6);
        cell(totals, "Professional Fee", label, SHADE);
        money(totals, q.getProfessionalFee(), body);
        cell(totals, "Taxes (" + (q.getTaxPercent() == null ? "0" : q.getTaxPercent().stripTrailingZeros().toPlainString()) + "%)", label, SHADE);
        money(totals, q.getTaxAmount(), body);
        cell(totals, "Travel / Other Expenses", label, SHADE);
        money(totals, q.getOtherExpenses(), body);
        cell(totals, "Total", white, NAVY);
        money(totals, q.getTotalAmount(), label);
        document.add(totals);

        section(document, "Payment Milestones", q.getPaymentMilestones(), heading, body);
        Paragraph note = new Paragraph("This quotation was generated on BDS Marketplace. Payment and contractual arrangements are "
                + "finalised directly between the purchaser and the service provider outside the platform.", body);
        note.setSpacingBefore(14);
        document.add(note);
        document.close();
        return out.toByteArray();
    }

    private static void section(Document document, String title, String content, Font heading, Font body) {
        if (content == null || content.isBlank()) {
            return;
        }
        Paragraph head = new Paragraph(title, heading);
        head.setSpacingBefore(10);
        head.setSpacingAfter(2);
        document.add(head);
        document.add(new Paragraph(content, body));
    }

    private static void cell(PdfPTable table, String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(207, 219, 228));
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        table.addCell(cell);
    }

    private static void money(PdfPTable table, BigDecimal value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : "INR " + NumberFormat.getNumberInstance(new Locale("en", "IN"))
                .format(value), font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(new Color(207, 219, 228));
        table.addCell(cell);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
