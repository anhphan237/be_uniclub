package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.CashoutResponse;
import com.example.uniclub.enums.CashoutStatusEnum;
import com.example.uniclub.service.ClubCashoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Rút điểm CLB", description = "Quản lý đơn xin rút điểm quy đổi ra tiền mặt của CLB")
public class CashoutController {

    private final ClubCashoutService cashoutService;

    // ================= CLUB LEADER =================

    @Operation(summary = "Gửi đơn xin rút điểm CLB")
    @PostMapping("/api/cashouts")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ApiResponse<CashoutResponse> requestCashout(
            @RequestParam Long clubId,
            @RequestParam Long points,
            @RequestParam(required = false) String note
    ) {
        return ApiResponse.ok(
                cashoutService.requestCashout(clubId, points, note)
        );
    }

    @Operation(summary = "Xem danh sách đơn rút điểm của CLB")
    @GetMapping("/api/cashouts/my-club/{clubId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ApiResponse<List<CashoutResponse>> myClubCashouts(
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(
                cashoutService.getCashoutsByClub(clubId)
        );
    }

    // ================= UNI STAFF / ADMIN =================

    @Operation(summary = "Xem danh sách đơn rút điểm đang chờ duyệt")
    @GetMapping("/api/admin/cashouts/pending")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<List<CashoutResponse>> pending() {
        return ApiResponse.ok(
                cashoutService.getPendingCashouts()
        );
    }

    @Operation(summary = "Duyệt đơn xin rút điểm CLB")
    @PostMapping("/api/admin/cashouts/{id}/approve")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<Void> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String note
    ) {
        cashoutService.approveCashout(id, note);
        return ApiResponse.ok();
    }

    @Operation(summary = "Từ chối đơn xin rút điểm CLB")
    @PostMapping("/api/admin/cashouts/{id}/reject")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<Void> reject(
            @PathVariable Long id,
            @RequestParam String reason
    ) {
        cashoutService.rejectCashout(id, reason);
        return ApiResponse.ok();
    }


    @Operation(summary = "Xem lịch sử đơn rút điểm (approved / rejected)")
    @GetMapping("/api/cashouts/my-club/{clubId}/history")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ApiResponse<List<CashoutResponse>> history(
            @PathVariable Long clubId,
            @RequestParam CashoutStatusEnum status
    ) {
        return ApiResponse.ok(
                cashoutService.getCashoutsByClubAndStatus(clubId, status)
        );
    }

    @Operation(summary = "Xem chi tiết đơn rút điểm")
    @GetMapping("/api/cashouts/{id}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<CashoutResponse> detail(
            @PathVariable Long id
    ) {
        return ApiResponse.ok(
                cashoutService.getCashoutDetail(id)
        );
    }

    @Operation(summary = "Xem danh sách đơn đã duyệt")
    @GetMapping("/api/admin/cashouts/approved")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<List<CashoutResponse>> approved() {
        return ApiResponse.ok(
                cashoutService.getApprovedCashouts()
        );
    }

    @Operation(summary = "Xem danh sách đơn bị từ chối")
    @GetMapping("/api/admin/cashouts/rejected")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<List<CashoutResponse>> rejected() {
        return ApiResponse.ok(
                cashoutService.getRejectedCashouts()
        );
    }

    @Operation(summary = "Xem tất cả đơn rút điểm CLB")
    @GetMapping("/api/admin/cashouts")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ApiResponse<List<CashoutResponse>> allCashouts() {
        return ApiResponse.ok(
                cashoutService.getAllCashouts()
        );
    }

}
