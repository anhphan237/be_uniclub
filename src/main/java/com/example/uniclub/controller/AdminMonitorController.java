package com.example.uniclub.controller;

import com.example.uniclub.dto.response.SystemStatusResponse;
import com.example.uniclub.service.AdminMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/monitor")
@RequiredArgsConstructor
public class AdminMonitorController {

    private final AdminMonitorService monitorService;

    @Operation(summary = "Kiểm tra trạng thái hệ thống")
    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        return ResponseEntity.ok(monitorService.getSystemStatus());
    }
}
