package com.example.uniclub.service;

import com.example.uniclub.dto.response.ProductMediaResponse;
import java.util.List;

public interface ProductMediaService {
    List<ProductMediaResponse> addMedia(Long productId, List<String> urls, String type, boolean thumbnail);
    ProductMediaResponse updateMedia(Long productId, Long mediaId, String url, Boolean thumbnail, Integer displayOrder);
    void removeMedia(Long mediaId);
    List<ProductMediaResponse> listMedia(Long productId);
}
