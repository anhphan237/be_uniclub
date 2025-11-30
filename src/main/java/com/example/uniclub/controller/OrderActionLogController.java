package com.example.uniclub.controller;

import com.example.uniclub.dto.response.OrderActionLogResponse;
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

    // 🔹 Get all logs
    @GetMapping
    @Operation(summary = "Get all order action logs")
    public ResponseEntity<List<OrderActionLogResponse>> getAllLogs() {
        return ResponseEntity.ok(logService.getAllLogs());
    }

    // 🔹 Get logs by target user
    @GetMapping("/user/{userId}/order/{orderId}")
    @Operation(summary = "Get order logs by userId and orderId")
    public ResponseEntity<List<OrderActionLogResponse>> getLogsByTargetUserAndOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {

        return ResponseEntity.ok(logService.getLogsByTargetUserAndOrder(userId, orderId));
    }

    @GetMapping("/membership/{membershipId}/order/{orderId}")
    @Operation(summary = "Get order logs by membershipId and orderId")
    public ResponseEntity<List<OrderActionLogResponse>> getLogsByMembershipAndOrder(
            @PathVariable Long membershipId,
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                logService.getLogsByTargetMembershipAndOrder(membershipId, orderId)
        );
    }

    // 🔹 OPTIONAL: Get logs by actor (staff/leader)
    @GetMapping("/actor/{actorId}")
    @Operation(summary = "Get order logs by actor user Id")
    public ResponseEntity<List<OrderActionLogResponse>> getLogsByActor(
            @PathVariable Long actorId) {

        return ResponseEntity.ok(logService.getLogsByActor(actorId));
    }
}

