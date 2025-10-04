package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductCreateRequest req){
        return ResponseEntity.ok(ApiResponse.ok(productService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.ok(productService.get(id)));
    }

    @GetMapping
    public ResponseEntity<?> list(Pageable pageable){
        return ResponseEntity.ok(productService.list(pageable));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(@PathVariable Long id,
                                                                    @RequestParam Integer stock){
        return ResponseEntity.ok(ApiResponse.ok(productService.updateStock(id, stock)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }
}
