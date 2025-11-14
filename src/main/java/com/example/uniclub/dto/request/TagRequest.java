package com.example.uniclub.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagRequest {
    private String name;
    private String description;
    private Boolean core;  // Optional – chỉ staff set khi tạo
}
