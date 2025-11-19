package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.request.ClubRenameRequest;
import com.example.uniclub.dto.request.ClubUpdateRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(
        name = "Club Management (CLUB / ADMIN / STAFF)",
        description = """
        Quản lý thông tin các câu lạc bộ (CLB) bao gồm:
        - Tạo mới, xem chi tiết, thống kê, xóa CLB
        - Lấy danh sách CLB, số lượng thành viên, sự kiện được duyệt
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final EventRepository eventRepo;

    // ==========================================================
    // 🟢 1. TẠO CLB MỚI
    // ==========================================================
    @Operation(
            summary = "Tạo CLB mới",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Tạo mới 1 CLB trong hệ thống với các thông tin cơ bản (tên, mô tả, hình ảnh...).
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Tạo CLB thành công")
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClubResponse>> create(
            @Valid @RequestBody ClubCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.create(req)));
    }

    // ==========================================================
    // 🔵 2. LẤY THÔNG TIN CHI TIẾT 1 CLB
    // ==========================================================
    @Operation(
            summary = "Xem thông tin chi tiết CLB",
            description = """
                Public API.<br>
                Trả về chi tiết CLB bao gồm số lượng thành viên ACTIVE.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy thông tin thành công")
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubResponse>> get(@PathVariable Long id) {
        ClubResponse club = clubService.get(id);
        long memberCount = membershipRepo.countByClub_ClubIdAndState(id, MembershipStateEnum.ACTIVE);
        club.setMemberCount(memberCount);
        return ResponseEntity.ok(ApiResponse.ok(club));
    }

    // ==========================================================
    // 🟣 3. LẤY DANH SÁCH CLB (PHÂN TRANG)
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách CLB (phân trang)",
            description = """
                Public API.<br>
                Cho phép lọc và phân trang danh sách các CLB đang hoạt động trong hệ thống.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy danh sách thành công")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.list(pageable)));
    }

    // ==========================================================
    // 🔴 4. XOÁ CLB (ADMIN)
    // ==========================================================
    @Operation(
            summary = "Xóa CLB khỏi hệ thống",
            description = """
                Chỉ dành cho **ADMIN**.<br>
                Thực hiện xóa (soft delete hoặc hard delete tùy config) CLB khỏi hệ thống.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Xóa CLB thành công")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        clubService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted successfully"));
    }

    // ==========================================================
    // 🟡 5. THỐNG KÊ TOÀN HỆ THỐNG CLB
    // ==========================================================
    @Operation(
            summary = "Thống kê toàn hệ thống CLB",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về tổng số CLB, tổng số thành viên, thành viên đang ACTIVE và số sự kiện được duyệt.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy thống kê thành công")
    )
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClubs", clubRepo.count());
        stats.put("totalMembers", membershipRepo.count());
        stats.put("activeMembers", membershipRepo.countByState(MembershipStateEnum.ACTIVE));
        stats.put("approvedEvents", eventRepo.countByStatus(EventStatusEnum.APPROVED));

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ==========================================================
    // 🧩 6. LẤY TỔNG SỐ THÀNH VIÊN ACTIVE CỦA 1 CLB
    // ==========================================================
    @Operation(
            summary = "Lấy tổng số thành viên đang ACTIVE trong CLB",
            description = """
                Public API.<br>
                Trả về số lượng thành viên có trạng thái ACTIVE trong CLB.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy dữ liệu thành công")
    )
    @GetMapping("/{id}/member-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberCount(@PathVariable Long id) {
        long count = membershipRepo.countByClub_ClubIdAndState(id, MembershipStateEnum.ACTIVE);
        Map<String, Object> result = new HashMap<>();
        result.put("clubId", id);
        result.put("activeMemberCount", count);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    @Operation(
            summary = "Lấy danh sách CLB mà người dùng có thể apply",
            description = """
            Public API.<br>
            Trả về danh sách CLB mà user **chưa tham gia hoặc chưa chờ duyệt**.
            Có hỗ trợ tìm kiếm theo tên (keyword) và phân trang.
            """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy danh sách thành công")
    )
    @GetMapping("/available-for-apply")
    public ResponseEntity<ApiResponse<?>> getAvailableForApply(
            @RequestParam Long userId, // hoặc lấy từ token nếu bạn có @AuthenticationPrincipal
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.getAvailableForApply(userId, keyword, pageable)));
    }
    // ==========================================================
// 🟠 4.1 ĐỔI TÊN CLB (ADMIN / UNIVERSITY_STAFF / CLUB_LEADER)
// ==========================================================
    @Operation(
            summary = "Đổi tên CLB",
            description = """
            Cho phép **ADMIN**, **UNIVERSITY_STAFF** hoặc **CLUB_LEADER** đổi tên CLB.<br>
            Yêu cầu truyền `newName` trong body.
            """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Đổi tên CLB thành công")
    )
    @PutMapping("/{clubId}/rename")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClubResponse>> renameClub(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubRenameRequest req,
            @AuthenticationPrincipal com.example.uniclub.security.CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.renameClub(clubId, req, user.getUserId())));
    }



    @PutMapping("/{clubId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cập nhật thông tin CLB",
            description = """
        ADMIN / UNIVERSITY_STAFF: được chỉnh toàn bộ thông tin.<br>
        CLUB_LEADER: chỉ được chỉnh name/description/vision/major.
        """
    )
    public ResponseEntity<ApiResponse<ClubResponse>> updateClub(
            @PathVariable Long clubId,
            @RequestBody ClubUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubService.updateClub(clubId, req, user.getUserId())
        ));
    }

}
