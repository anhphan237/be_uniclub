package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminProductResponse;
import com.example.uniclub.dto.response.AdminRedeemOrderResponse;
import com.example.uniclub.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    // ====================================================
    // 🧾 1️⃣ Lấy danh sách sản phẩm (phân trang)
    // ====================================================
    @Operation(summary = "Get paginated list of all products")
    @GetMapping
    public ResponseEntity<Page<AdminProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminProductService.getAllProducts(PageRequest.of(page, size)));
    }

    // ====================================================
    // 🛠️ 2️⃣ Bật/tắt hoạt động của sản phẩm
    // ====================================================
    @Operation(summary = "Toggle product active/inactive by ID")
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleProductActive(@PathVariable Long id) {
        adminProductService.toggleProductActive(id);
        return ResponseEntity.ok().build();
    }

    // ====================================================
    // 📦 3️⃣ Lấy danh sách đơn redeem (phân trang)
    // ====================================================
    @Operation(summary = "Get paginated list of all redeem orders")
    @GetMapping("/orders")
    public ResponseEntity<Page<AdminRedeemOrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminProductService.getAllOrders(PageRequest.of(page, size)));
    }

    // ====================================================
    // 🔍 4️⃣ Xem chi tiết 1 đơn redeem
    // ====================================================
    @Operation(summary = "Get detail of a redeem order by ID")
    @GetMapping("/orders/{id}")
    public ResponseEntity<AdminRedeemOrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminProductService.getOrderDetail(id));
    }
}
