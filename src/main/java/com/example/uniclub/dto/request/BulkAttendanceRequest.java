package com.example.uniclub.dto.request;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkAttendanceRequest {
    private List<MemberAttendance> records;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberAttendance {
        private Long membershipId;
        private String status; // PRESENT / LATE / ABSENT / EXCUSED
        private String note;
    }
}
