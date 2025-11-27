package com.example.uniclub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.uniclub.dto.response.ReturnImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadAvatar(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "uniclub/avatars",
                        "resource_type", "image",
                        "overwrite", true
                ));
        return uploadResult.get("secure_url").toString();
    }

    public String uploadBackground(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "uniclub/backgrounds",
                        "resource_type", "image",
                        "overwrite", true
                ));
        return uploadResult.get("secure_url").toString();
    }

    public String uploadProductMedia(MultipartFile file, Long productId) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "uniclub/products/" + productId,
                        "resource_type", "auto",
                        "overwrite", true
                )
        );
        return uploadResult.get("secure_url").toString();
    }
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from Cloudinary: " + publicId, e);
        }
    }
    public Map<?, ?> uploadRefundImageRaw(MultipartFile file, Long orderId) throws IOException {
        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "uniclub/refunds/order-" + orderId,
                        "resource_type", "image",
                        "overwrite", false
                )
        );
    }

    public void deleteRefundImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete refund image: " + publicId);
        }
    }

}