package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminProductResponse;
import com.example.uniclub.dto.response.AdminRedeemOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminProductService {
    Page<AdminProductResponse> getAllProducts(Pageable pageable);
    void disableProduct(Long id);
    Page<AdminRedeemOrderResponse> getAllOrders(Pageable pageable);
    AdminRedeemOrderResponse getOrderDetail(Long id);
    void toggleProductActive(Long productId);

}
