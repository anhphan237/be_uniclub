package com.example.uniclub.dto;

import com.example.uniclub.enums.NotificationTypeEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {
    private Long userId;
    private String message;
    private NotificationTypeEnum type;
}
