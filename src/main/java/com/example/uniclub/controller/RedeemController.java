package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.RedeemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redeem")
@RequiredArgsConstructor
public class RedeemController {

    private final RedeemService redeemService;

    // Member tự đặt hàng từ kho CLB (trừ điểm ngay, status = PENDING)
    @PostMapping("/club/{clubId}/order")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<OrderResponse>> createClubOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId,
            @RequestBody RedeemOrderRequest req) {
        OrderResponse res = redeemService.createClubOrder(clubId, req, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // STAFF thực hiện đổi tại booth của event (trừ điểm và COMPLETE ngay)
    @PostMapping("/event/{eventId}/redeem")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> eventRedeem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId,
            @RequestBody RedeemOrderRequest req) {
        OrderResponse res = redeemService.eventRedeem(eventId, req, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // Staff/Leader xác nhận đơn PENDING thành COMPLETED (hàng CLB)
    @PutMapping("/order/{orderId}/complete")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long orderId) {
        OrderResponse res = redeemService.complete(orderId, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    // Staff/Leader xử lý hoàn điểm (hàng lỗi/không đúng mô tả)
    @PutMapping("/order/{orderId}/refund")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> refund(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long orderId) {
        OrderResponse res = redeemService.refund(orderId, principal.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
    @GetMapping("/orders/member")
    @PreAuthorize("hasAnyRole('MEMBER','STUDENT','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByMember(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.getOrdersByMember(principal.getUser().getUserId())
        ));
    }

    @GetMapping("/orders/club/{clubId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listClubOrders(
            @PathVariable Long clubId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.getOrdersByClub(clubId)
        ));
    }
    @GetMapping("/orders/event/{eventId}")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listEventOrders(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.getOrdersByEvent(eventId)
        ));
    }
    @PutMapping("/order/{orderId}/refund-partial")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<OrderResponse>> refundPartial(
            @PathVariable Long orderId,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                redeemService.refundPartial(orderId, quantity, principal.getUser().getUserId())
        ));
    }

}
