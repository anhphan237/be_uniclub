package com.example.uniclub.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCheckinRequest {
    // ⚙️ Token JWT từ QR code (bắt buộc)
    private String eventJwtToken;

    // 🏅 Cấp độ tham dự (NONE / GOOD / EXCELLENT / STAFF)
    private String level;
}
