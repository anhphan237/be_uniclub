package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminProductResponse;
import com.example.uniclub.dto.response.AdminProductStatsResponse;
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
@Tag(name = "Quản lý Sản phẩm & Đổi quà")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final RedeemService redeemService;

    // =======================
    // GET all products
    // =======================
    @GetMapping
    @Operation(summary = "Xem danh sách tất cả sản phẩm")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<Page<AdminProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminProductService.getAllProducts(PageRequest.of(page, size))
        );
    }

    // =======================
    // TOGGLE ACTIVE
    // =======================
    @PutMapping("/{id}/toggle")
    @Operation(summary = "Bật/Tắt trạng thái ACTIVE/INACTIVE")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleProductActive(@PathVariable Long id) {
        adminProductService.toggleProductActive(id);
        return ResponseEntity.ok().build();
    }

    // =======================
    // ARCHIVE
    // =======================
    @PutMapping("/{id}/archive")
    @Operation(summary = "Lưu trữ sản phẩm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveProduct(@PathVariable Long id) {
        adminProductService.archiveProduct(id);
        return ResponseEntity.ok().build();
    }

    // =======================
    // ACTIVATE
    // =======================
    @PutMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt sản phẩm (force ACTIVE)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateProduct(@PathVariable Long id) {
        adminProductService.activateProduct(id);
        return ResponseEntity.ok().build();
    }

    // =======================
    // DEACTIVATE
    // =======================
    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Vô hiệu hóa sản phẩm (force INACTIVE)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        adminProductService.deactivateProduct(id);
        return ResponseEntity.ok().build();
    }

    // =======================
    // STATS
    // =======================
    @GetMapping("/stats")
    @Operation(summary = "Thống kê số lượng sản phẩm theo trạng thái")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProductStatsResponse> getStats() {
        return ResponseEntity.ok(adminProductService.getStats());
    }

    // =======================
    // ORDERS
    // =======================
    @GetMapping("/orders")
    @Operation(summary = "Danh sách đơn redeem")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<Page<AdminRedeemOrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminProductService.getAllOrders(PageRequest.of(page, size))
        );
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Chi tiết đơn redeem")
    public ResponseEntity<AdminRedeemOrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminProductService.getOrderDetail(id));
    }

    @GetMapping("/users/{userId}/redeem-history")
    @Operation(summary = "Lịch sử đổi quà của một user")
    public ResponseEntity<List<OrderResponse>> getUserRedeemHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(redeemService.getOrdersByMember(userId));
    }
}
