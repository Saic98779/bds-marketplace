package com.sidbi.bds.common.report;

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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReportExporter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public ResponseEntity<byte[]> export(ReportTable table, ReportFormat format, String fileBaseName) {
        byte[] content = switch (format) {
            case XLSX -> toExcel(table);
            case PDF -> toPdf(table);
            case CSV -> toCsv(table);
        };
        String fileName = fileBaseName + "-" + LocalDate.now() + "." + format.getExtension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(format.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(content);
    }

    public byte[] toExcel(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");
            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(table.getTitle() + " (generated " + LocalDateTime.now().format(STAMP) + ")");
            Row header = sheet.createRow(2);
            List<String> headers = table.getHeaders();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 3;
            for (List<Object> values : table.getRows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    Cell cell = row.createCell(i);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate Excel report", ex);
        }
    }

    public byte[] toPdf(ReportTable table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(table.getHeaders().size() > 6 ? PageSize.A4.rotate() : PageSize.A4, 24, 24, 28, 28);
        PdfWriter.getInstance(document, out);
        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(11, 42, 66));
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(46, 74, 95));
        Paragraph title = new Paragraph("BDS Marketplace - " + table.getTitle(), titleFont);
        title.setSpacingAfter(4);
        document.add(title);
        Paragraph stamp = new Paragraph("Generated " + LocalDateTime.now().format(STAMP), bodyFont);
        stamp.setSpacingAfter(10);
        document.add(stamp);
        PdfPTable pdfTable = new PdfPTable(table.getHeaders().size());
        pdfTable.setWidthPercentage(100);
        pdfTable.setHeaderRows(1);
        for (String h : table.getHeaders()) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new Color(11, 110, 142));
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            pdfTable.addCell(cell);
        }
        boolean shade = false;
        for (List<Object> values : table.getRows()) {
            for (Object value : values) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : String.valueOf(value), bodyFont));
                cell.setPadding(3);
                if (shade) {
                    cell.setBackgroundColor(new Color(241, 245, 248));
                }
                pdfTable.addCell(cell);
            }
            shade = !shade;
        }
        document.add(pdfTable);
        document.close();
        return out.toByteArray();
    }

    public byte[] toCsv(ReportTable table) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", table.getHeaders().stream().map(ReportExporter::csvCell).toList())).append("\n");
        for (List<Object> values : table.getRows()) {
            builder.append(String.join(",", values.stream().map(v -> csvCell(v == null ? "" : String.valueOf(v))).toList())).append("\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvCell(String value) {
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
