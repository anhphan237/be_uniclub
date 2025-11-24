package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.ClubActivityMonthlyResponse;
import com.example.uniclub.dto.response.ClubActivityRankingItemResponse;
import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.MemberActivityQueryService;
import com.example.uniclub.service.MemberActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
@Tag(name = "Admin Activity", description = "Thống kê mức độ hoạt động của các CLB toàn trường")
public class AdminActivityController {

    private final JwtUtil jwtUtil;
    private final MemberActivityQueryService memberActivityQueryService;
    private final MemberActivityService memberActivityService;

    private void ensureAdminOrStaff(User user) {
        String roleName = user.getRole().getRoleName();
        boolean ok = "ADMIN".equalsIgnoreCase(roleName)
                || "UNIVERSITY_STAFF".equalsIgnoreCase(roleName);
        if (!ok) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only ADMIN or UNIVERSITY_STAFF can access this resource.");
        }
    }

    // =============== Ranking CLB theo hoạt động ===============
    @GetMapping("/clubs/ranking")
    @Operation(
            summary = "Ranking CLB theo mức độ hoạt động trong 1 tháng",
            description = "Dùng cho Admin / University Staff"
    )
    public ResponseEntity<ApiResponse<List<ClubActivityRankingItemResponse>>> getClubRanking(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureAdminOrStaff(current);

        var ym = YearMonth.of(year, month);

        List<ClubActivityRankingItemResponse> data =
                memberActivityQueryService.getClubRanking(ym);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // =============== Chi tiết hoạt động 1 CLB ===============
    @GetMapping("/clubs/{clubId}/detail")
    @Operation(
            summary = "Xem chi tiết hoạt động member của 1 CLB theo tháng",
            description = "Tương tự API của Leader nhưng mở cho Admin/Staff"
    )
    public ResponseEntity<ApiResponse<ClubActivityMonthlyResponse>> getClubActivityDetail(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureAdminOrStaff(current);

        YearMonth ym = YearMonth.of(year, month);

        ClubActivityMonthlyResponse data =
                memberActivityQueryService.getClubActivity(clubId, ym);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // =============== Tính lại toàn bộ cho tất cả CLB ===============
    @PostMapping("/recalculate-all")
    @Operation(
            summary = "Tính lại mức độ hoạt động của member cho tất cả CLB",
            description = "Dùng cho Admin / University Staff để rebuild dữ liệu 1 tháng"
    )
    public ResponseEntity<ApiResponse<String>> recalculateAllClubs(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureAdminOrStaff(current);

        YearMonth ym = YearMonth.of(year, month);

        memberActivityService.recalculateForAllClubsAndMonth(ym);

        String msg = "Recalculated activity for all clubs in month " + ym;
        return ResponseEntity.ok(ApiResponse.ok(msg));
    }
}
