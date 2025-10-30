package com.example.uniclub.dto.response;

import com.example.uniclub.enums.PointRequestStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointRequestResponse {

    private Long id;
    private String clubName;
    private Integer requestedPoints;
    private String reason;
    private PointRequestStatusEnum status;
    private String staffNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
