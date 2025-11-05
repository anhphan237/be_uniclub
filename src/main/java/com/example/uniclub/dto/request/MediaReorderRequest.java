package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MediaReorderRequest(
        @NotEmpty List<Long> orderedMediaIds  // danh sách mediaId theo thứ tự mới (0..n)
) {}
