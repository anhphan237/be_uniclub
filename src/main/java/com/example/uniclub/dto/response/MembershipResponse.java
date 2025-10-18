package com.example.uniclub.dto.response;

import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MembershipResponse {
    private Long membershipId;
    private Long userId;
    private Long clubId;

    private ClubRoleEnum clubRole;
    private MembershipStateEnum state;

    private boolean staff;
    private LocalDate joinedDate;
    private LocalDate endDate;

    private String fullName;
    private String studentCode;
    private String clubName;

    private String email;
    private String avatarUrl;
    private String major;
}
