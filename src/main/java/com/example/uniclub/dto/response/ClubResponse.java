package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String majorPolicyName;
    private Long majorId;
    private String majorName;
    private Long leaderId;
    private String leaderName;
    private Long memberCount;
    public static ClubResponse fromEntity(com.example.uniclub.entity.Club club) {
        if (club == null) return null;

        return ClubResponse.builder()
                .id(club.getClubId())
                .name(club.getName())
                .description(club.getDescription())
                .majorId(club.getMajor() != null ? club.getMajor().getId() : null)
                .majorName(club.getMajor() != null ? club.getMajor().getName() : null)
                .leaderId(club.getLeader() != null ? club.getLeader().getUserId() : null)
                .leaderName(club.getLeader() != null ? club.getLeader().getFullName() : null)
                .memberCount(club.getMemberCount() != null ? club.getMemberCount().longValue() : 0L)
                .build();
    }

}
