package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ProductMediaResponse;
import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductMedia;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ProductMediaRepository;
import com.example.uniclub.repository.ProductRepository;
import com.example.uniclub.service.CloudinaryService;
import com.example.uniclub.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductMediaServiceImpl implements ProductMediaService {

    private final ProductRepository productRepo;
    private final ProductMediaRepository mediaRepo;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ProductMediaResponse uploadMedia(Long productId, MultipartFile file) throws IOException {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        // Upload lên Cloudinary
        String url = cloudinaryService.uploadProductMedia(file, productId);

        // Xác định loại media
        String contentType = file.getContentType();
        String type = (contentType != null && contentType.startsWith("video")) ? "VIDEO" : "IMAGE";

        // Lưu vào DB
        ProductMedia m = ProductMedia.builder()
                .product(p)
                .url(url)
                .type(type)
                .isThumbnail(false)
                .displayOrder(p.getMediaList().size())
                .build();

        mediaRepo.save(m);

        return new ProductMediaResponse(
                m.getMediaId(),
                m.getUrl(),
                m.getType(),
                m.isThumbnail(),
                m.getDisplayOrder()
        );
    }

    @Override
    @Transactional
    public void removeMedia(Long mediaId) {
        ProductMedia m = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        mediaRepo.delete(m);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductMediaResponse> listMedia(Long productId) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        return mediaRepo.findByProductOrderByDisplayOrderAscMediaIdAsc(p)
                .stream()
                .map(m -> new ProductMediaResponse(
                        m.getMediaId(),
                        m.getUrl(),
                        m.getType(),
                        m.isThumbnail(),
                        m.getDisplayOrder()
                ))
                .toList();
    }
}
