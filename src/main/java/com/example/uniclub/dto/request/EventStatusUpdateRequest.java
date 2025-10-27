package com.example.uniclub.dto.request;

import com.example.uniclub.enums.EventStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventStatusUpdateRequest {
    @NotNull
    private EventStatusEnum status;
    private Integer budgetPoints;
}
