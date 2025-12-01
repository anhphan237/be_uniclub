package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminProductResponse;
import com.example.uniclub.dto.response.AdminRedeemOrderResponse;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.service.AdminProductService;
import com.example.uniclub.service.RedeemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "Quản lý Sản phẩm & Đổi quà", description = "ADMIN và UNI_STAFF có thể xem dữ liệu, chỉ ADMIN được chỉnh sửa.")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final RedeemService redeemService;

    // ====================================================
    // 1. Lấy danh sách tất cả sản phẩm (phân trang)
    // ====================================================
    @Operation(
            summary = "Xem danh sách tất cả sản phẩm (ADMIN + UNI_STAFF)",
            description = """
                    API cho phép ADMIN và UNI_STAFF xem tất cả sản phẩm trong hệ thống.

                    - Hỗ trợ phân trang: page (mặc định = 0), size (mặc định = 10)
                    - Trả về thông tin sản phẩm: ID, tên, CLB sở hữu, trạng thái, loại, tồn kho, điểm đổi, lượt đổi.

                    Quyền truy cập:
                    - ADMIN
                    - UNI_STAFF
                    """
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    public ResponseEntity<Page<AdminProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminProductService.getAllProducts(PageRequest.of(page, size)));
    }

    // ====================================================
    // 2. Bật / Tắt trạng thái hoạt động của sản phẩm
    // ====================================================
    @Operation(
            summary = "Bật / Tắt trạng thái hoạt động của sản phẩm (CHỈ ADMIN)",
            description = """
                    ADMIN có thể kích hoạt hoặc vô hiệu hóa một sản phẩm.

                    - Trạng thái sẽ chuyển giữa ACTIVE ↔ INACTIVE.
                    - UNI_STAFF KHÔNG được phép thao tác.

                    Quyền truy cập:
                    - Chỉ ADMIN
                    """
    )
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleProductActive(@PathVariable Long id) {
        adminProductService.toggleProductActive(id);
        return ResponseEntity.ok().build();
    }

    // ====================================================
    // 3. Lấy danh sách toàn bộ đơn đổi quà (phân trang)
    // ====================================================
    @Operation(
            summary = "Xem danh sách tất cả đơn đổi quà (ADMIN + UNI_STAFF)",
            description = """
                    API cho phép ADMIN và UNI_STAFF xem toàn bộ đơn đổi quà.

                    - Hỗ trợ phân trang
                    - Trả về: mã đơn, sản phẩm, người đổi, CLB, điểm, trạng thái, thời gian.

                    Quyền truy cập:
                    - ADMIN
                    - UNI_STAFF
                    """
    )
    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    public ResponseEntity<Page<AdminRedeemOrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminProductService.getAllOrders(PageRequest.of(page, size)));
    }

    // ====================================================
    // 4. Xem chi tiết một đơn đổi quà cụ thể
    // ====================================================
    @Operation(
            summary = "Xem chi tiết đơn đổi quà (ADMIN + UNI_STAFF)",
            description = """
                    API cho phép ADMIN và UNI_STAFF xem đầy đủ thông tin của một đơn đổi quà.

                    Trả về:
                    - Mã đơn, sản phẩm, người đổi, CLB, số lượng, tổng điểm
                    - QR code, thời gian tạo, thời gian hoàn thành

                    Quyền truy cập:
                    - ADMIN
                    - UNI_STAFF
                    """
    )
    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    public ResponseEntity<AdminRedeemOrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminProductService.getOrderDetail(id));
    }

    // ====================================================
    // 5. Xem lịch sử đổi quà của một người dùng cụ thể
    // ====================================================
    @Operation(
            summary = "Xem lịch sử đổi quà của một người dùng (ADMIN + UNI_STAFF)",
            description = """
                    API cho phép ADMIN và UNI_STAFF xem toàn bộ lịch sử đổi quà của một người dùng.

                    Trả về danh sách:
                    - mã đơn, sản phẩm, điểm, CLB, trạng thái, thời gian

                    Quyền truy cập:
                    - ADMIN
                    - UNI_STAFF
                    """
    )
    @GetMapping("/users/{userId}/redeem-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    public ResponseEntity<List<OrderResponse>> getUserRedeemHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(redeemService.getOrdersByMember(userId));
    }
}
