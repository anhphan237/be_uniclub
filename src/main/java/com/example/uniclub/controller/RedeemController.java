package com.example.uniclub.controller;

import com.example.uniclub.dto.request.RedeemRequest;
import com.example.uniclub.dto.response.RedeemResponse;
import com.example.uniclub.entity.Redeem;
import com.example.uniclub.service.DeliverService;
import com.example.uniclub.service.RedeemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redeems")
@RequiredArgsConstructor
public class RedeemController {

    private final RedeemService redeemService;
    private final DeliverService deliverService;

    /**
     * Member dùng điểm để đổi quà
     */
    @PostMapping
    public ResponseEntity<RedeemResponse> redeemProduct(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestBody RedeemRequest request
    ) {
        Redeem redeem = redeemService.redeemProduct(userId,
                request.getProductId(), request.getQuantity(), request.getEventId());

        RedeemResponse response = RedeemResponse.builder()
                .redeemId(redeem.getRedeemId())
                .productName(redeem.getProduct().getName())
                .quantity(redeem.getQuantity())
                .totalCostPoints(redeem.getTotalCostPoints())
                .status(redeem.getStatus().name())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Staff xác nhận giao quà
     */
    @PostMapping("/{redeemId}/deliver")
    public ResponseEntity<String> deliverReward(
            @AuthenticationPrincipal(expression = "membershipId") Long staffMembershipId,
            @PathVariable Long redeemId
    ) {
        deliverService.deliver(staffMembershipId, redeemId);
        return ResponseEntity.ok("Redeem " + redeemId + " delivered successfully.");
    }
}
