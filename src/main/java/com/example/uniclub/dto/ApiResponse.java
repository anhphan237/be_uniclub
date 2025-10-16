package com.example.uniclub.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    // ✅ Trả response thành công có data
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("success")
                .data(data)
                .build();
    }

    // ✅ Trả response thành công KHÔNG có data
    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder()
                .success(true)
                .message("success")
                .data(null)
                .build();
    }

    // ✅ Trả response thành công chỉ có message
    public static <T> ApiResponse<T> msg(String m) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(m)
                .build();
    }

    // ✅ Trả response lỗi
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
