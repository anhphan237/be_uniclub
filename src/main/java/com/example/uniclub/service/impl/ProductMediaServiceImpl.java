package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductMedia;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ProductMediaRepository;
import com.example.uniclub.repository.ProductRepository;
import com.example.uniclub.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductMediaServiceImpl implements ProductMediaService {

    private final ProductRepository productRepo;
    private final ProductMediaRepository mediaRepo;

    @Override
    @Transactional
    public List<ProductResponse.MediaItem> addMedia(Long productId, List<String> urls, String type, boolean thumbnail) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int baseOrder = p.getMediaList().size();

        for (int i = 0; i < urls.size(); i++) {
            ProductMedia m = ProductMedia.builder()
                    .product(p)
                    .url(urls.get(i))
                    .type(type)
                    .isThumbnail(thumbnail && i == 0)
                    .displayOrder(baseOrder + i)
                    .build();
            p.addMedia(m);
        }
        productRepo.save(p);

        return mediaRepo.findByProductOrderByDisplayOrderAscMediaIdAsc(p).stream()
                .map(m -> new ProductResponse.MediaItem(m.getMediaId(), m.getUrl(), m.getType(), m.isThumbnail(), m.getDisplayOrder()))
                .toList();
    }

    @Override
    @Transactional
    public void removeMedia(Long mediaId) {
        ProductMedia m = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        mediaRepo.delete(m);
    }

    @Override
    public List<ProductResponse.MediaItem> listMedia(Long productId) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        return mediaRepo.findByProductOrderByDisplayOrderAscMediaIdAsc(p).stream()
                .map(m -> new ProductResponse.MediaItem(m.getMediaId(), m.getUrl(), m.getType(), m.isThumbnail(), m.getDisplayOrder()))
                .toList();
    }
}
