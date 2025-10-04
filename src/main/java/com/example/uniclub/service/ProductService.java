package com.example.uniclub.service;

import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse create(ProductCreateRequest req);
    ProductResponse get(Long id);
    Page<ProductResponse> list(Pageable pageable);
    ProductResponse updateStock(Long id, Integer stock);
    void delete(Long id);
}
