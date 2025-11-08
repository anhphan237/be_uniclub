package com.example.uniclub.controller;



import com.example.uniclub.dto.response.AdminSummaryResponse;
import com.example.uniclub.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = " Tổng hợp dữ liệu hệ thống cho Admin Dashboard")
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminDashboardService.getSummary());
    }
}
