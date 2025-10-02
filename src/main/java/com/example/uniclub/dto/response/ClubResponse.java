package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String majorPolicyName;
}
