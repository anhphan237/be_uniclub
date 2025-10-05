package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum ReturnStatusEnum {
    REQUESTED("REQUESTED", "Return has been requested"),
    APPROVED("APPROVED", "Return request approved"),
    REJECTED("REJECTED", "Return request rejected"),
    REFUNDED("REFUNDED", "Points refunded after return");

    private final String code;
    private final String description;

    ReturnStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

