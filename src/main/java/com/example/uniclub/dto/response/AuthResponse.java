package com.example.uniclub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // ✅ ẩn mọi field có giá trị null khỏi JSON
public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String role;

    // ✅ các field liên quan tới CLB
    private Long clubId;
    private List<Long> clubIds;
    private boolean requirePasswordChange;

    // ✅ chỉ xuất hiện khi không null (MEMBER hoặc staff CLB)
    private Boolean staff;
}
