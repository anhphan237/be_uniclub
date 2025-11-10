package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventBudgetApproveRequest {

    @NotNull(message = "Approved budget points is required")
    @Min(value = 0, message = "Approved budget points must be non-negative")
    private Long approvedBudgetPoints;
}
