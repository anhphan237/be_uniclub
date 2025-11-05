package com.example.uniclub.dto.request;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.Min;

public record ProductMediaUpdateRequest(
        MultipartFile newFile,   // optional: up ảnh/video mới
        Boolean isThumbnail,     // optional
        @Min(0) Integer displayOrder, // optional
        String type              // optional: "IMAGE" | "VIDEO"
) {}
