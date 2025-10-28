package com.example.uniclub.dto.response;

import com.example.uniclub.enums.PointRequestStatusEnum;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PointRequestResponse {
    private Long id;
    private String clubName;
    private Integer requestedPoints;
    private String reason;
    private PointRequestStatusEnum status;
    private String staffNote;
}
