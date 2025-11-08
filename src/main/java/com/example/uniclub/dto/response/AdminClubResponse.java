package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClubResponse {
    private Long id;
    private String name;
    private String description;
    private String majorName;
    private String leaderName;
    private String leaderEmail;
    private int memberCount;
    private int eventCount;
    private boolean active;
}
