package com.example.uniclub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KickMemberRequest {

    @Schema(description = "Reason for removing the member", example = "Vi phạm nội quy nhiều lần")
    private String reason;
}
