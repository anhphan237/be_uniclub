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

    // START/MID/END
    private String level;
}
