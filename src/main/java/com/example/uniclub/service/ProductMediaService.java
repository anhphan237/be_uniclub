package com.example.uniclub.service;

import com.example.uniclub.dto.response.ProductMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductMediaService {

    ProductMediaResponse uploadMedia(Long productId, MultipartFile file) throws IOException;

    void removeMedia(Long mediaId);

    List<ProductMediaResponse> listMedia(Long productId);

}
