package com.waalert.whatsapp_alert_backend.service;

import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final DynamicDataSourceManager dataSourceManager;

    public byte[] generateExcel(String reportName, String sql) throws IOException {
        DataSource ds = dataSourceManager.getActiveDataSource();

        try (Connection conn     = ds.getConnection();
             Statement  stmt     = conn.createStatement();
             XSSFWorkbook wb     = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            ResultSet rs    = stmt.executeQuery(sql);
            Sheet     sheet = wb.createSheet(cap(reportName, 31));

            // Styles
            CellStyle hStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true); hFont.setColor(IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // Header row
            Row header = sheet.createRow(0);
            header.setHeightInPoints(20);
            for (int i = 1; i <= cols; i++) {
                Cell c = header.createCell(i - 1);
                c.setCellValue(meta.getColumnLabel(i));
                c.setCellStyle(hStyle);
            }

            // Data rows
            int rn = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rn);
                for (int i = 1; i <= cols; i++) {
                    Cell c = row.createCell(i - 1);
                    Object v = rs.getObject(i);
                    if (v instanceof Number n) c.setCellValue(n.doubleValue());
                    else c.setCellValue(v != null ? v.toString() : "");
                    if (rn % 2 == 0) c.setCellStyle(altStyle);
                }
                rn++;
            }

            for (int i = 0; i < cols; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 15_000) sheet.setColumnWidth(i, 15_000);
            }
            sheet.createFreezePane(0, 1);
            wb.write(out);
            log.info("Excel '{}' generated — {} rows", reportName, rn - 1);
            return out.toByteArray();

        } catch (SQLException e) {
            throw new RuntimeException("Excel generation failed: " + e.getMessage());
        }
    }

    public byte[] generateCsv(String sql) {
        DataSource ds = dataSourceManager.getActiveDataSource();
        StringBuilder csv = new StringBuilder("\uFEFF"); // UTF-8 BOM
        try (Connection conn = ds.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= cols; i++) headers.add(esc(meta.getColumnLabel(i)));
            csv.append(String.join(",", headers)).append("\r\n");

            while (rs.next()) {
                List<String> vals = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    vals.add(esc(v != null ? v.toString() : ""));
                }
                csv.append(String.join(",", vals)).append("\r\n");
            }
        } catch (SQLException e) {
            throw new RuntimeException("CSV generation failed: " + e.getMessage());
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String esc(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private String cap(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
