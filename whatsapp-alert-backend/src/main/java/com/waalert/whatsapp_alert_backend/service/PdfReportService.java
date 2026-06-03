package com.waalert.whatsapp_alert_backend.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PdfReportService — generates professional PDF reports using OpenPDF.
 *
 * Called by:
 *   ReportController → POST /api/reports/download-pdf
 *   ReportController → POST /api/reports/send-whatsapp-pdf
 *   AnalyticsController → POST /api/analytics/download-pdf
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    private final DynamicDataSourceManager dataSourceManager;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Colour palette
    private static final Color HEADER_BG     = new Color(0x12, 0x8C, 0x7E);  // WhatsApp dark green
    private static final Color HEADER_ALT_BG = new Color(0x25, 0xD3, 0x66);  // WhatsApp green
    private static final Color ROW_ALT_BG    = new Color(0xF0, 0xF7, 0xF4);
    private static final Color BORDER_COLOR  = new Color(0xCC, 0xCC, 0xCC);
    private static final Color FOOTER_COLOR  = new Color(0x88, 0x88, 0x88);

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Execute a SQL query and render results as a PDF.
     *
     * @param reportName  title shown in the PDF header
     * @param sql         SELECT query to execute
     * @return            raw PDF bytes
     */
    public byte[] generateFromQuery(String reportName, String sql) {
        DataSource ds = dataSourceManager.getActiveDataSource();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(5_000);
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // Collect columns
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= cols; i++) columns.add(meta.getColumnLabel(i));

            // Collect rows
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    row.add(v != null ? v.toString() : "");
                }
                rows.add(row);
            }

            buildPdf(reportName, sql, columns, rows, out);
            log.info("PDF '{}' generated — {} rows, {} cols", reportName, rows.size(), cols);

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /**
     * Build a PDF from pre-fetched column/row data (used by Analytics).
     */
    public byte[] generateFromData(String reportName, String sql,
                                    List<String> columns, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            buildPdf(reportName, sql, columns, rows, out);
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF builder
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPdf(String reportName, String sql,
                           List<String> columns, List<List<String>> rows,
                           ByteArrayOutputStream out) throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 60, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // Page event for header/footer on every page
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                addPageHeaderFooter(w, d, reportName);
            }
        });

        doc.open();

        // ── Title block ───────────────────────────────────────────────────
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, HEADER_BG);
        Font subFont   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font sqlFont   = new Font(Font.COURIER,   8,  Font.NORMAL, Color.DARK_GRAY);

        Paragraph title = new Paragraph(reportName, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph genTime = new Paragraph(
                "Generated: " + LocalDateTime.now().format(DT_FMT), subFont);
        genTime.setAlignment(Element.ALIGN_CENTER);
        genTime.setSpacingAfter(4);
        doc.add(genTime);

        Paragraph totalRec = new Paragraph(
                "Total Records: " + rows.size(), subFont);
        totalRec.setAlignment(Element.ALIGN_CENTER);
        totalRec.setSpacingAfter(12);
        doc.add(totalRec);

        // ── SQL query section ─────────────────────────────────────────────
        if (sql != null && !sql.isBlank()) {
            PdfPTable sqlBox = new PdfPTable(1);
            sqlBox.setWidthPercentage(100);
            PdfPCell sqlCell = new PdfPCell();
            sqlCell.setBackgroundColor(new Color(0xF5, 0xF5, 0xF5));
            sqlCell.setBorderColor(BORDER_COLOR);
            sqlCell.setPadding(8);

            Paragraph sqlLabel = new Paragraph("SQL Query:", subFont);
            Paragraph sqlText  = new Paragraph(sql.trim(), sqlFont);
            sqlCell.addElement(sqlLabel);
            sqlCell.addElement(sqlText);
            sqlBox.addCell(sqlCell);
            sqlBox.setSpacingAfter(14);
            doc.add(sqlBox);
        }

        // ── Data table ────────────────────────────────────────────────────
        if (columns.isEmpty()) {
            doc.add(new Paragraph("No data returned by the query.", subFont));
        } else {
            int numCols = columns.size();
            PdfPTable table = new PdfPTable(numCols);
            table.setWidthPercentage(100);

            // Equal column widths
            float[] widths = new float[numCols];
            for (int i = 0; i < numCols; i++) widths[i] = 1f;
            table.setWidths(widths);

            // Header row
            Font colHeaderFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            for (String col : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(col, colHeaderFont));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(6);
                cell.setBorderColor(Color.WHITE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data rows
            Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            for (int r = 0; r < rows.size(); r++) {
                Color bg = (r % 2 == 0) ? Color.WHITE : ROW_ALT_BG;
                for (String val : rows.get(r)) {
                    PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", dataFont));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(5);
                    cell.setBorderColor(BORDER_COLOR);
                    table.addCell(cell);
                }
            }

            doc.add(table);
        }

        doc.close();
    }

    /** Adds top header bar and bottom footer on every page. */
    // private void addPageHeaderFooter(PdfWriter writer, Document document, String reportName) {
    //     PdfContentByte cb = writer.getDirectContent();
    //     float pageWidth  = document.getPageSize().getWidth();
    //     float pageHeight = document.getPageSize().getHeight();

    //     // Header bar
    //     cb.setColorFill(HEADER_BG);
    //     cb.rectangle(0, pageHeight - 40, pageWidth, 40);
    //     cb.fill();

    //     cb.setFontAndSize(BaseFont.createFont(), 11);
    //     cb.setColorFill(Color.WHITE);
    //     cb.beginText();
    //     cb.setTextMatrix(20, pageHeight - 26);
    //     cb.showText("Smart Alert System  |  " + reportName);
    //     cb.endText();

    //     // Footer line
    //     cb.setColorStroke(BORDER_COLOR);
    //     cb.setLineWidth(0.5f);
    //     cb.moveTo(20, 40);
    //     cb.lineTo(pageWidth - 20, 40);
    //     cb.stroke();

    //     Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, FOOTER_COLOR);
    //     cb.beginText();
    //     cb.setFontAndSize(BaseFont.createFont(), 8);
    //     cb.setColorFill(FOOTER_COLOR);
    //     cb.setTextMatrix(20, 26);
    //     cb.showText("Generated by Smart Alert System  •  " +
    //             LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    //     cb.setTextMatrix(pageWidth - 60, 26);
    //     cb.showText("Page " + writer.getCurrentPageNumber());
    //     cb.endText();
    // }
    private void addPageHeaderFooter(PdfWriter writer, Document document, String reportName) {

    try {

        PdfContentByte cb = writer.getDirectContent();

        float pageWidth = document.getPageSize().getWidth();
        float pageHeight = document.getPageSize().getHeight();

        // Header background
        cb.setColorFill(HEADER_BG);
        cb.rectangle(0, pageHeight - 40, pageWidth, 40);
        cb.fill();

        // Header text
        BaseFont baseFont = BaseFont.createFont(
                BaseFont.HELVETICA,
                BaseFont.CP1252,
                BaseFont.NOT_EMBEDDED
        );

        cb.beginText();
        cb.setFontAndSize(baseFont, 11);
        cb.setColorFill(Color.WHITE);
        cb.setTextMatrix(20, pageHeight - 26);
        cb.showText("Smart Alert System  |  " + reportName);
        cb.endText();

        // Footer line
        cb.setColorStroke(BORDER_COLOR);
        cb.setLineWidth(0.5f);
        cb.moveTo(20, 40);
        cb.lineTo(pageWidth - 20, 40);
        cb.stroke();

        // Footer text
        cb.beginText();
        cb.setFontAndSize(baseFont, 8);
        cb.setColorFill(FOOTER_COLOR);

        cb.setTextMatrix(20, 26);
        cb.showText(
                "Generated by Smart Alert System  •  " +
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
                )
        );

        cb.setTextMatrix(pageWidth - 60, 26);
        cb.showText("Page " + writer.getCurrentPageNumber());

        cb.endText();

    } catch (Exception e) {
        log.error("Error while adding PDF header/footer", e);
    }
}
}