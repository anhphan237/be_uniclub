package com.example.uniclub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String role;

    private Long clubId;          // dùng cho CLUB_LEADER
    private List<Long> clubIds;   // dùng cho MEMBER
    private Boolean staff;

}
