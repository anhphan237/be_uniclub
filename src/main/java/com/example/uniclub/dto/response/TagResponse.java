package com.example.uniclub.dto.response;

import com.example.uniclub.entity.Tag;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {

    private Long tagId;
    private String name;
    private String description;
    private boolean core;

    public static TagResponse from(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getTagId())
                .name(tag.getName())
                .description(tag.getDescription())
                .core(tag.isCore())
                .build();
    }
}
