package com.example.uniclub.dto.request;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubAttendanceSessionRequest {
    private LocalDate date;        // Ngày sinh hoạt
    private LocalTime startTime;   // Giờ bắt đầu
    private LocalTime endTime;     // Giờ kết thúc
    private String note;           // Ghi chú (tuỳ chọn)
}
