package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum ProductTxStatusEnum {
    RESERVED("RESERVED", "Product reserved, waiting to be fulfilled"),
    FULFILLED("FULFILLED", "Product successfully delivered or used"),
    RETURNED("RETURNED", "Product returned to inventory"),
    CANCELED("CANCELED", "Transaction was canceled");

    private final String code;
    private final String description;

    ProductTxStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

