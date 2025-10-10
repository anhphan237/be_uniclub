package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberCreateRequest {

    @NotNull
    private Long userId;   // id của user muốn thêm vào CLB

    @NotNull
    private Long clubId;   // id CLB

    private String level;  // ví dụ: BASIC, SILVER, GOLD
}
