package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum RegistrationStatusEnum {

    PENDING("PENDING", "The user has submitted a registration request and is waiting for confirmation."),
    CONFIRMED("CONFIRMED", "The user has successfully registered and the participation is confirmed."),
    CHECKED_IN("CHECKED_IN", "The user has checked in to the event."),
    REFUNDED("REFUNDED", "The user has received a refund and reward points after the event is completed."),
    CANCELED("CANCELED", "The user canceled the registration or was removed due to absence.");

    private final String code;
    private final String description;

    RegistrationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
