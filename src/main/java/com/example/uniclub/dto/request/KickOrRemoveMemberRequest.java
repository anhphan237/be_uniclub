package com.example.uniclub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KickOrRemoveMemberRequest {

    @Schema(
            description = "Reason for removing this member from the club",
            example = "Vi phạm nội quy CLB nhiều lần"
    )
    private String reason;
}
