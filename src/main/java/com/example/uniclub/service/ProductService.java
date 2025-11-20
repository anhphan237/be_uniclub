package com.example.uniclub.service;

import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.request.ProductUpdateRequest;
import com.example.uniclub.dto.response.EventProductResponse;
import com.example.uniclub.dto.response.EventValidityResponse;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.ProductStockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse create(ProductCreateRequest req, Long clubId);
    ProductResponse get(Long id);

    // ✅ List cho admin / unistaff (phân trang)
    Page<ProductResponse> list(Pageable pageable);

    // ✅ List cho CLB (phân trang)
    Page<ProductResponse> listByClub(Long clubId, Pageable pageable, Boolean includeInactive, Boolean includeArchived);
    Page<ProductResponse> adminFilterList(Pageable pageable, String status, String type, String tag, String keyword);

    // ✅ List cho CLB (không phân trang)
    List<ProductResponse> listByClub(Long clubId, boolean includeInactive, boolean includeArchived);

    ProductResponse update(Long id, ProductUpdateRequest req);
    ProductResponse updateStock(Long id, Integer delta, String note); // delta: + nhập / - trừ
    void delete(Long id); // soft-delete => INACTIVE
    ProductResponse updateProduct(Long clubId, Long productId, ProductUpdateRequest req);

    List<ProductResponse> searchByTags(List<String> tagNames);

    // Lịch sử nhập hàng
    List<ProductStockHistory> getStockHistory(Long productId);

    ProductResponse activateProduct(Long productId);

    EventValidityResponse checkEventValidity(Long productId);
    List<EventProductResponse> listEventProductsByClub(Long clubId);

}
