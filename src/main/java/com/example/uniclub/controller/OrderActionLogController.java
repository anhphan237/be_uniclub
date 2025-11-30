package com.example.uniclub.controller;

import com.example.uniclub.entity.OrderActionLog;
import com.example.uniclub.service.OrderActionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order-logs")
@RequiredArgsConstructor
@Tag(name = "Order Action Logs")
public class OrderActionLogController {

    private final OrderActionLogService logService;

    // 🔹 1. Get all logs
    @GetMapping
    @Operation(summary = "Get all order action logs")
    public ResponseEntity<List<OrderActionLog>> getAllLogs() {
        return ResponseEntity.ok(logService.getAllLogs());
    }

    // 🔹 2. Get logs by target user (người redeem)
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get order logs by userId (target user – người redeem)")
    public ResponseEntity<List<OrderActionLog>> getLogsByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(logService.getLogsByTargetUser(userId));
    }

    // 🔹 3. OPTIONAL – Get logs by actor (staff/leader xử lý)
    @GetMapping("/actor/{actorId}")
    @Operation(summary = "Get order logs by actor userId (người thực hiện hành động)")
    public ResponseEntity<List<OrderActionLog>> getLogsByActor(
            @PathVariable Long actorId) {

        return ResponseEntity.ok(logService.getLogsByActor(actorId));
    }
}

