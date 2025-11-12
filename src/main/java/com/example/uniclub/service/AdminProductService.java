package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminProductResponse;
import com.example.uniclub.dto.response.AdminRedeemOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminProductService {

    // 📦 Danh sách tất cả sản phẩm (phân trang)
    Page<AdminProductResponse> getAllProducts(Pageable pageable);

    // 🚫 Vô hiệu hóa (ẩn) sản phẩm
    void disableProduct(Long id);

    // 🔁 Danh sách tất cả đơn redeem (phân trang)
    Page<AdminRedeemOrderResponse> getAllOrders(Pageable pageable);

    // 🔍 Xem chi tiết 1 đơn redeem
    AdminRedeemOrderResponse getOrderDetail(Long id);

    // 🔘 Bật/tắt hoạt động của sản phẩm
    void toggleProductActive(Long productId);
}
