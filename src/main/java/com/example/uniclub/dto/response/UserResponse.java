package com.example.uniclub.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String roleName;
    private String status;
    private String studentCode;
    private String majorName;
    private String bio;
    private String avatarUrl;
    private String backgroundUrl;

    private List<ClubInfo> clubs;
    private WalletResponse wallet;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClubInfo {
        private Long clubId;
        private String clubName;
    }
}
