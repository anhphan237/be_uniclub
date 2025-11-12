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

/**
 * Controller quản lý Sản phẩm và Đơn đổi quà (chỉ dành cho ADMIN).
 * Cung cấp các chức năng:
 * - Quản lý trạng thái sản phẩm (kích hoạt / vô hiệu hóa)
 * - Xem danh sách sản phẩm
 * - Xem và tra cứu đơn đổi quà (redeem orders)
 * - Xem lịch sử đổi quà của từng người dùng
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "Quản lý Sản phẩm & Đổi quà (ADMIN)", description = "Các API cho phép ADMIN quản lý sản phẩm và đơn đổi quà trong hệ thống")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final RedeemService redeemService;

    // ====================================================
    // 1. Lấy danh sách tất cả sản phẩm (phân trang)
    // ====================================================
    @Operation(
            summary = "Lấy danh sách tất cả sản phẩm (phân trang)",
            description = """
                    API này cho phép ADMIN xem toàn bộ danh sách sản phẩm trong hệ thống.

                    Tham số truy vấn:
                    - page (mặc định = 0): số trang bắt đầu từ 0.
                    - size (mặc định = 10): số lượng phần tử mỗi trang.

                    Kết quả trả về gồm các thông tin:
                    - Mã sản phẩm, tên sản phẩm, CLB sở hữu, trạng thái, loại, số lượng tồn kho, điểm đổi, lượt đổi quà.

                    Quyền hạn: chỉ ADMIN được phép truy cập.
                    """
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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
            summary = "Bật hoặc tắt hoạt động của sản phẩm",
            description = """
                    API cho phép ADMIN kích hoạt hoặc vô hiệu hóa một sản phẩm.

                    Tham số đường dẫn:
                    - id: ID của sản phẩm cần thay đổi trạng thái.

                    Khi bật/tắt, hệ thống sẽ thay đổi giữa hai trạng thái:
                    - ACTIVE ↔ INACTIVE

                    Quyền hạn: chỉ ADMIN được phép thực hiện.
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
            summary = "Lấy danh sách toàn bộ đơn đổi quà (phân trang)",
            description = """
                    API cho phép ADMIN xem toàn bộ lịch sử các đơn đổi quà (redeem orders) trong hệ thống.

                    Tham số truy vấn:
                    - page: số trang (bắt đầu từ 0)
                    - size: số lượng đơn mỗi trang

                    Kết quả trả về:
                    - Mã đơn hàng, tên sản phẩm, người đổi quà, CLB, số lượng, tổng điểm, trạng thái, thời gian tạo và hoàn thành.

                    Quyền hạn: chỉ ADMIN được phép sử dụng API này.
                    """
    )
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
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
            summary = "Xem chi tiết một đơn đổi quà",
            description = """
                    API cho phép ADMIN xem thông tin chi tiết của một đơn đổi quà cụ thể.

                    Tham số đường dẫn:
                    - id: ID của đơn hàng cần xem.

                    Kết quả trả về gồm:
                    - Mã đơn hàng, tên sản phẩm, người đổi, CLB, số lượng, tổng điểm, trạng thái, ảnh QR, thời gian tạo và hoàn thành.

                    Quyền hạn: chỉ ADMIN được phép truy cập.
                    """
    )
    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminRedeemOrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminProductService.getOrderDetail(id));
    }

    // ====================================================
    // 5. Xem lịch sử đổi quà của một người dùng cụ thể
    // ====================================================
    @Operation(
            summary = "Xem lịch sử đổi quà của người dùng",
            description = """
                    API cho phép ADMIN tra cứu toàn bộ lịch sử đổi quà của một người dùng cụ thể.

                    Tham số đường dẫn:
                    - userId: ID của người dùng cần xem lịch sử.

                    Kết quả trả về là danh sách các đơn hàng đã đổi:
                    - Mã đơn hàng, sản phẩm, số lượng, tổng điểm, trạng thái, thời gian tạo, CLB sở hữu sản phẩm.

                    Quyền hạn: chỉ ADMIN được phép truy cập API này.
                    """
    )
    @GetMapping("/users/{userId}/redeem-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getUserRedeemHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(redeemService.getOrdersByMember(userId));
    }
}
