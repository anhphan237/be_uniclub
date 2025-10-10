package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipResponse {

    private Long membershipId; // ✅ cần có để builder hiểu
    private Long userId;       // id của thành viên
    private Long clubId;       // id CLB
    private String level;      // BASIC, SILVER, GOLD
    private String state;      // trạng thái tham gia
    private boolean staff;     // true nếu là staff CLB
}
