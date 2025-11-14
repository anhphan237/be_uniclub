package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.service.MultiplierPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/university/multiplier-policies")
@RequiredArgsConstructor
public class MultiplierPolicyController {

    private final MultiplierPolicyService service;

    // ===============================================================
    // 1️⃣ LẤY TOÀN BỘ CHÍNH SÁCH NHÂN ĐIỂM
    // ===============================================================
    @Operation(
            summary = "Lấy danh sách tất cả chính sách nhân điểm (Multiplier Policies)",
            description = """
                    API này dành cho **ADMIN** và **UNIVERSITY_STAFF**.<br><br>
                    • Trả về **toàn bộ danh sách chính sách nhân điểm** trong hệ thống.<br>
                    • Bao gồm: loại áp dụng (CLUB / MEMBER), loại hoạt động, ruleName, hệ số,...<br>
                    • Không phân biệt đang active hay không.<br><br>
                    Đây là API dùng cho trang quản trị để xem toàn bộ danh sách policy.
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<List<MultiplierPolicyResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // ===============================================================
    // 2️⃣ LẤY CHI TIẾT POLICY THEO ID
    // ===============================================================
    @Operation(
            summary = "Lấy chi tiết 1 chính sách nhân điểm theo ID",
            description = """
                    • Trả về chi tiết 1 policy: mục tiêu (CLUB / MEMBER), loại hoạt động, ruleName, mô tả,...<br>
                    • Dùng trong trang chỉnh sửa policy.<br><br>
                    Nếu ID không tồn tại → trả về lỗi 404.
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // ===============================================================
    // 3️⃣ LỌC POLICY THEO LOẠI (CLUB / MEMBER)
    // ===============================================================
    @Operation(
            summary = "Lọc danh sách chính sách theo loại đối tượng (CLUB hoặc MEMBER)",
            description = """
                    Trả về danh sách **chính sách đang active** cho:<br>
                    • CLUB (áp dụng khi club nhận thưởng, sự kiện, ngân sách,...)<br>
                    • MEMBER (áp dụng cho điểm thưởng cá nhân, hoạt động tháng,...)<br><br>
                    API này dùng cho trang quản trị hiển thị theo từng tab CLUB/MEMBER.
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/target/{type}")
    public ResponseEntity<List<MultiplierPolicyResponse>> getByType(
            @PathVariable PolicyTargetTypeEnum type) {
        return ResponseEntity.ok(service.getActiveByTargetType(type));
    }

    // ===============================================================
    // 4️⃣ TẠO MỚI CHÍNH SÁCH
    // ===============================================================
    @Operation(
            summary = "Tạo mới một Multiplier Policy",
            description = """
                    • Dùng để thêm chính sách nhân điểm mới vào hệ thống.<br>
                    • Cho phép cấu hình:<br>
                      - targetType (CLUB / MEMBER)<br>
                      - activityType (loại hoạt động)<br>
                      - ruleName (Normal, Positive, Full...)<br>
                      - min / max threshold<br>
                      - hệ số multiplier<br>
                      - mô tả chính sách<br><br>
                    ⚠️ Hệ thống sẽ chặn tạo mới nếu trùng (targetType + activityType + ruleName).
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<MultiplierPolicyResponse> create(
            @RequestBody MultiplierPolicyRequest r) {
        return ResponseEntity.ok(service.create(r));
    }

    // ===============================================================
    // 5️⃣ CẬP NHẬT POLICY
    // ===============================================================
    @Operation(
            summary = "Cập nhật thông tin một chính sách nhân điểm",
            description = """
                    • Chỉnh sửa nội dung policy đã tồn tại.<br>
                    • Cho phép thay đổi:<br>
                      - ruleName<br>
                      - min/max threshold<br>
                      - multiplier<br>
                      - active/inactive<br>
                      - mô tả<br><br>
                    ⚠️ Chỉ admin/staff mới có quyền chỉnh sửa.<br>
                    ⚠️ Nếu đổi ruleName thành trùng với policy khác → báo lỗi 400.
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> update(
            @PathVariable Long id,
            @RequestBody MultiplierPolicyRequest r) {
        return ResponseEntity.ok(service.update(id, r));
    }

    // ===============================================================
    // 6️⃣ XÓA POLICY
    // ===============================================================
    @Operation(
            summary = "Xoá chính sách nhân điểm",
            description = """
                    • Xóa hoàn toàn policy khỏi Database.<br>
                    • Nếu ID không tồn tại → 404.<br><br>
                    ⚠️ Nên dùng cho các policy lỗi hoặc không còn sử dụng.
                    """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
