package com.example.uniclub.service;

import com.example.uniclub.dto.response.ProductResponse;

import java.util.List;

public interface ProductMediaService {
    List<ProductResponse.MediaItem> addMedia(Long productId, List<String> urls, String type, boolean thumbnail);
    void removeMedia(Long mediaId);
    List<ProductResponse.MediaItem> listMedia(Long productId);
}
