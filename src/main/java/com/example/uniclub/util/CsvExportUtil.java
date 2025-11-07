package com.example.uniclub.util;

import com.example.uniclub.entity.EventRegistration;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExportUtil {
    public static byte[] exportToCsv(List<EventRegistration> regs) {
        StringBuilder sb = new StringBuilder("Full Name,Email,Status,Check-in,Check-out\n");
        for (EventRegistration r : regs) {
            sb.append(r.getUser().getFullName()).append(",")
                    .append(r.getUser().getEmail()).append(",")
                    .append(r.getStatus()).append(",")
                    .append(r.getCheckinAt() != null ? r.getCheckinAt() : "").append(",")
                    .append(r.getCheckoutAt() != null ? r.getCheckoutAt() : "")
                    .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
