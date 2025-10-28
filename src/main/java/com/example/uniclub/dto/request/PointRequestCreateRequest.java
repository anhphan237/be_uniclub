package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PointRequestCreateRequest {
    @NotNull
    private Long clubId;

    @Positive
    private Integer requestedPoints;

    @NotBlank
    private String reason;
}
