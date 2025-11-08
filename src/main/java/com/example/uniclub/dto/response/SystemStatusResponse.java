package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {
    private boolean databaseUp;
    private boolean redisUp;
    private boolean rabbitmqUp;
    private boolean cloudinaryUp;
    private String appVersion;
    private String environment; // dev / staging / prod
    private String lastCheckedAt;
}
