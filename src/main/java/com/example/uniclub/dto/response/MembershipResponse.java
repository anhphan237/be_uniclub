package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class MembershipResponse {
    private Long id;
    private Long userId;
    private Long clubId;
    private String level;
    private String state;
}
