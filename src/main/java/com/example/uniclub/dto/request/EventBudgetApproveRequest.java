package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventBudgetApproveRequest {

    @NotNull
    @Min(0)
    private Integer approvedBudgetPoints;
}
