package com.waalert.whatsapp_alert_backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PdfService
 * ──────────────────────────────────────────────────────────────
 * Generates professional PDF reports using OpenPDF (LibrePDF).
 *
 * Features:
 *  - Company branding header
 *  - Alert message section
 *  - Query results table with alternating rows
 *  - Generated timestamp footer
 *  - Reusable for WhatsApp PDF attachments and downloads
 *
 * Usage:
 *   byte[] pdfBytes = pdfService.generateAlertPdf(
 *       "Salary Report",       // title
 *       "Dear Team, ...",      // alert message
 *       columns,               // List<String>
 *       rows,                  // List<Map<String,Object>>
 *       "WA Alert System"      // company name
 *   );
 */
@Service
@Slf4j
public class PdfService {

    // ── Brand colours ─────────────────────────────────────────
    private static final Color WA_GREEN   = new Color(37, 211, 102);   // #25D366
    private static final Color DARK_BLUE  = new Color(0, 21, 41);      // #001529
    private static final Color LIGHT_GREY = new Color(245, 245, 245);
    private static final Color ALT_ROW    = new Color(230, 247, 255);

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate a complete alert PDF with:
     *  - Header with company name and logo area
     *  - Report title
     *  - Alert message body
     *  - Data table (columns + rows)
     *  - Footer with generated timestamp
     *
     * @param title       report / alert title
     * @param message     the alert message text
     * @param columns     column headers for the data table
     * @param rows        data rows (list of maps: column → value)
     * @param companyName company/sender name shown in the header
     * @return PDF file bytes ready to write to disk or send via WhatsApp
     */
    public byte[] generateAlertPdf(String title,
                                    String message,
                                    List<String> columns,
                                    List<Map<String, Object>> rows,
                                    String companyName) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // ── Page event for header/footer on every page ─────────
            writer.setPageEvent(new HeaderFooterEvent(companyName, title));

            document.open();

            // ── 1. Report Title ────────────────────────────────────
            addTitle(document, title);

            // ── 2. Alert Message ───────────────────────────────────
            if (message != null && !message.isBlank()) {
                addMessageBlock(document, message);
            }

            // ── 3. Data Table ──────────────────────────────────────
            if (columns != null && !columns.isEmpty()) {
                addSectionHeader(document, "Query Results");
                addDataTable(document, columns, rows);
            }

            document.close();
            log.info("PDF generated: '{}' — {} rows", title, rows != null ? rows.size() : 0);

        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /**
     * Generate a simple text-only PDF (for WhatsApp plain alert without table).
     */
    public byte[] generateSimplePdf(String title, String message, String companyName) {
        return generateAlertPdf(title, message, null, null, companyName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Content builders
    // ─────────────────────────────────────────────────────────────────────────

    private void addTitle(Document doc, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(title, titleFont));
        cell.setBackgroundColor(DARK_BLUE);
        cell.setPadding(16);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleTable.addCell(cell);

        doc.add(titleTable);
        doc.add(new Paragraph(" "));   // spacer
    }

    private void addMessageBlock(Document doc, String message) throws DocumentException {
        // Green left-border alert box
        Font msgFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

        PdfPTable msgTable = new PdfPTable(1);
        msgTable.setWidthPercentage(100);
        msgTable.setSpacingBefore(10);
        msgTable.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell(new Phrase(message, msgFont));
        cell.setBackgroundColor(new Color(240, 255, 244));   // very light green
        cell.setPadding(14);
        cell.setBorderColorLeft(WA_GREEN);
        cell.setBorderWidthLeft(4f);
        cell.setBorderColorRight(new Color(240, 255, 244));
        cell.setBorderColorTop(new Color(240, 255, 244));
        cell.setBorderColorBottom(new Color(240, 255, 244));
        cell.setBorderWidth(1f);
        msgTable.addCell(cell);

        doc.add(msgTable);
    }

    private void addSectionHeader(Document doc, String label) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, WA_GREEN);
        Paragraph p = new Paragraph(label, headerFont);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);

        // Thin green underline
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lc = new PdfPCell();
        lc.setBackgroundColor(WA_GREEN);
        lc.setFixedHeight(2f);
        lc.setBorder(Rectangle.NO_BORDER);
        line.addCell(lc);
        doc.add(line);
        doc.add(new Paragraph(" "));
    }

    private void addDataTable(Document doc, List<String> columns,
                               List<Map<String, Object>> rows) throws DocumentException {

        if (columns == null || columns.isEmpty()) return;
        List<Map<String, Object>> safeRows = rows != null ? rows : List.of();

        int colCount = columns.size();
        PdfPTable table = new PdfPTable(colCount);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        // Try to set equal column widths
        float[] widths = new float[colCount];
        for (int i = 0; i < colCount; i++) widths[i] = 1f;
        table.setWidths(widths);

        // ── Column headers ────────────────────────────────────
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String col : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
            cell.setBackgroundColor(DARK_BLUE);
            cell.setPadding(7);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(Color.WHITE);
            table.addCell(cell);
        }

        // ── Data rows ─────────────────────────────────────────
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        int rowNum = 0;
        int limit  = Math.min(safeRows.size(), 500);  // cap at 500 rows in PDF

        for (int r = 0; r < limit; r++) {
            Map<String, Object> row = safeRows.get(r);
            Color bg = (rowNum % 2 == 0) ? Color.WHITE : ALT_ROW;

            for (String col : columns) {
                Object val = row.get(col);
                String text = val != null ? val.toString() : "";

                PdfPCell cell = new PdfPCell(new Phrase(text, dataFont));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(LIGHT_GREY);
                table.addCell(cell);
            }
            rowNum++;
        }

        if (safeRows.size() > 500) {
            Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
            PdfPCell note = new PdfPCell(
                new Phrase("... and " + (safeRows.size() - 500) + " more rows (truncated in PDF)", noteFont));
            note.setColspan(colCount);
            note.setPadding(6);
            note.setBorderColor(LIGHT_GREY);
            table.addCell(note);
        }

        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header / Footer on every page
    // ─────────────────────────────────────────────────────────────────────────

    private static class HeaderFooterEvent extends PdfPageEventHelper {

        private final String companyName;
        private final String reportTitle;
        private final String generatedAt;

        HeaderFooterEvent(String companyName, String reportTitle) {
            this.companyName = companyName;
            this.reportTitle = reportTitle;
            this.generatedAt = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // ── Header bar ─────────────────────────────────────
            cb.saveState();
            cb.setColorFill(new Color(37, 211, 102));   // WA_GREEN
            cb.rectangle(document.left(), document.top() + 10,
                         document.right() - document.left(), 8);
            cb.fill();
            cb.restoreState();

            // Company name (top-left)
            Font compFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(0, 21, 41));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(companyName, compFont),
                document.left(), document.top() + 22, 0);

            // Report title (top-centre)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(reportTitle, titleFont),
                (document.left() + document.right()) / 2, document.top() + 22, 0);

            // ── Footer ─────────────────────────────────────────
            Font footFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);

            // Generated timestamp (left)
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase("Generated: " + generatedAt + "  |  WA Alert System", footFont),
                document.left(), document.bottom() - 15, 0);

            // Page number (right)
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase("Page " + writer.getPageNumber(), footFont),
                document.right(), document.bottom() - 15, 0);

            // Thin footer line
            cb.saveState();
            cb.setColorStroke(new Color(37, 211, 102));
            cb.setLineWidth(0.5f);
            cb.moveTo(document.left(), document.bottom() - 5);
            cb.lineTo(document.right(), document.bottom() - 5);
            cb.stroke();
            cb.restoreState();
        }
    }
}