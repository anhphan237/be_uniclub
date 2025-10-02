package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Product;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ProductRepository;
import com.example.uniclub.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;

    private ProductResponse toResp(Product p){
        return ProductResponse.builder()
                .id(p.getProductId())
                .clubId(p.getClub()!=null? p.getClub().getClubId(): null)
                .name(p.getName())
                .description(p.getDescription())
                .pricePoints(p.getPricePoints())
                .stockQuantity(p.getStockQuantity())
                .build();
    }

    @Override
    public ProductResponse create(ProductCreateRequest req) {
        Product p = Product.builder()
                .club(Club.builder().clubId(req.clubId()).build())
                .name(req.name())
                .description(req.description())
                .pricePoints(req.pricePoints())
                .stockQuantity(req.stockQuantity())
                .build();
        return toResp(productRepo.save(p));
    }

    @Override
    public ProductResponse get(Long id) {
        return productRepo.findById(id).map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
    }

    @Override
    public Page<ProductResponse> list(Pageable pageable) {
        return productRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public ProductResponse updateStock(Long id, Integer stock) {
        var p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
        if (stock == null || stock < 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stock không hợp lệ");
        p.setStockQuantity(stock);
        return toResp(productRepo.save(p));
    }

    @Override
    public void delete(Long id) {
        if (!productRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại");
        productRepo.deleteById(id);
    }
}
