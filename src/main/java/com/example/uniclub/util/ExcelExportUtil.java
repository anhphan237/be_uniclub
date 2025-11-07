package com.example.uniclub.util;

import com.example.uniclub.entity.EventRegistration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExportUtil {
    public static byte[] exportToExcel(List<EventRegistration> regs) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance");
            Row header = sheet.createRow(0);
            String[] cols = {"Full Name", "Email", "Status", "Check-in", "Check-out"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            int rowIdx = 1;
            for (EventRegistration r : regs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getUser().getFullName());
                row.createCell(1).setCellValue(r.getUser().getEmail());
                row.createCell(2).setCellValue(r.getStatus().name());
                row.createCell(3).setCellValue(String.valueOf(r.getCheckinAt()));
                row.createCell(4).setCellValue(String.valueOf(r.getCheckoutAt()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }
}
