package com.example.uniclub.service;

import com.example.uniclub.dto.request.MediaReorderRequest;
import com.example.uniclub.dto.request.ProductMediaUpdateRequest;
import com.example.uniclub.dto.response.ProductMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductMediaService {

    ProductMediaResponse uploadMedia(Long productId, MultipartFile file) throws IOException;

    void removeMedia(Long mediaId);

    List<ProductMediaResponse> listMedia(Long productId);

    ProductMediaResponse updateMedia(Long productId, Long mediaId, ProductMediaUpdateRequest req) throws IOException;

    List<ProductMediaResponse> uploadMultiple(Long productId, List<MultipartFile> files) throws IOException;

    void reorder(Long productId, MediaReorderRequest req);

    void setThumbnail(Long productId, Long mediaId);

}
