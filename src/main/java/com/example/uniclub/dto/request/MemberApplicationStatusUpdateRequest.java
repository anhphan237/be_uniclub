package com.example.uniclub.dto.request;

import com.example.uniclub.enums.MemberApplyStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberApplicationStatusUpdateRequest {

    @NotNull(message = "Status cannot be null")
    private MemberApplyStatusEnum status;

    private String reason; // optional: lý do duyệt hoặc từ chối
}
