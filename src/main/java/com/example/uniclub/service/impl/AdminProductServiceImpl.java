package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminProductResponse;
import com.example.uniclub.dto.response.AdminRedeemOrderResponse;
import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductOrder;
import com.example.uniclub.enums.ProductStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ProductOrderRepository;
import com.example.uniclub.repository.ProductRepository;
import com.example.uniclub.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepo;
    private final ProductOrderRepository orderRepo;

    // ============================================================
    // 📦 Danh sách tất cả sản phẩm (phân trang)
    // ============================================================
    @Override
    public Page<AdminProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepo.findAll(pageable);
        return products.map(this::toProductResp);
    }

    // ============================================================
    // 🚫 Vô hiệu hóa (ẩn) sản phẩm vi phạm
    // ============================================================
    @Override
    public void disableProduct(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        product.setStatus(ProductStatusEnum.INACTIVE);
        product.setIsActive(false);
        productRepo.save(product);
    }

    // ============================================================
    // 📜 Danh sách tất cả đơn redeem (phân trang)
    // ============================================================
    @Override
    public Page<AdminRedeemOrderResponse> getAllOrders(Pageable pageable) {
        Page<ProductOrder> orders = orderRepo.findAllByOrderByCreatedAtDesc(pageable);
        return orders.map(this::toOrderResp);
    }

    // ============================================================
    // 🔍 Chi tiết 1 đơn redeem
    // ============================================================
    @Override
    public AdminRedeemOrderResponse getOrderDetail(Long id) {
        ProductOrder order = orderRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        return toOrderResp(order);
    }

    // ============================================================
    // 🔘 Bật/tắt hoạt động sản phẩm
    // ============================================================
    @Override
    public void toggleProductActive(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        product.setIsActive(!product.getIsActive());
        product.setStatus(product.getIsActive()
                ? ProductStatusEnum.ACTIVE
                : ProductStatusEnum.INACTIVE);
        productRepo.save(product);
    }

    // ============================================================
    // 🧩 Helper Mapping Methods
    // ============================================================
    private AdminProductResponse toProductResp(Product p) {
        return AdminProductResponse.builder()
                .id(p.getProductId())
                .productCode(p.getProductCode())
                .name(p.getName())
                .clubName(p.getClub() != null ? p.getClub().getName() : "Unknown Club")
                .status(p.getStatus().name())
                .type(p.getType().name())
                .stockQuantity(p.getStockQuantity())
                .pointCost(p.getPointCost())
                .redeemCount(p.getRedeemCount())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private AdminRedeemOrderResponse toOrderResp(ProductOrder o) {
        return AdminRedeemOrderResponse.builder()
                .id(o.getOrderId())
                .orderCode(o.getOrderCode())
                .productName(o.getProduct() != null ? o.getProduct().getName() : "Unknown Product")
                .buyerName(o.getMembership() != null && o.getMembership().getUser() != null
                        ? o.getMembership().getUser().getFullName()
                        : "Unknown User")
                .clubName(o.getClub() != null ? o.getClub().getName() : "Unknown Club")
                .quantity(o.getQuantity())
                .totalPoints(o.getTotalPoints())
                .status(o.getStatus() != null ? o.getStatus().name() : "UNKNOWN")
                .createdAt(o.getCreatedAt())
                .completedAt(o.getCompletedAt())
                .qrCodeBase64(o.getQrCodeBase64())
                .build();
    }
}
