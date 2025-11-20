package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MediaReorderRequest;
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
import com.example.uniclub.dto.request.ProductMediaUpdateRequest;

import java.io.IOException;
import java.util.ArrayList;
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

        if (m.getUrl() != null && m.getUrl().contains("cloudinary.com")) {
            try {
                String[] parts = m.getUrl().split("/");
                String publicId = parts[parts.length - 1].split("\\.")[0];
                cloudinaryService.deleteFile("uniclub/products/" + m.getProduct().getProductId() + "/" + publicId);
            } catch (Exception ignored) {}
        }

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
    @Override
    @Transactional
    public ProductMediaResponse updateMedia(Long productId, Long mediaId, ProductMediaUpdateRequest req) throws IOException {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        ProductMedia media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));

        if (!media.getProduct().getProductId().equals(productId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Media does not belong to this product");

        // 1️⃣ Nếu có file mới → XÓA ảnh cũ, upload ảnh mới
        if (req.newFile() != null && !req.newFile().isEmpty()) {
            // Nếu DB có publicId thì xóa khỏi Cloudinary
            if (media.getUrl() != null && media.getUrl().contains("cloudinary.com")) {
                try {
                    String[] parts = media.getUrl().split("/");
                    String publicId = parts[parts.length - 1].split("\\.")[0]; // lấy tên file không kèm .jpg
                    cloudinaryService.deleteFile("uniclub/products/" + productId + "/" + publicId);
                } catch (Exception e) {
                    // Không gây lỗi hệ thống nếu không xóa được
                }
            }

            // Upload file mới
            String newUrl = cloudinaryService.uploadProductMedia(req.newFile(), productId);
            media.setUrl(newUrl);

            // Cập nhật type IMAGE/VIDEO
            String contentType = req.newFile().getContentType();
            if (contentType != null) {
                media.setType(contentType.startsWith("video") ? "VIDEO" : "IMAGE");
            }
        }

        // 2️⃣ Cập nhật metadata
        if (req.type() != null) media.setType(req.type());
        if (req.displayOrder() != null) media.setDisplayOrder(req.displayOrder());
        if (req.isThumbnail() != null) media.setThumbnail(req.isThumbnail());

        mediaRepo.save(media);

        // 3️⃣ Nếu set thumbnail = true → reset các ảnh khác
        if (Boolean.TRUE.equals(req.isThumbnail())) {
            mediaRepo.findByProductOrderByDisplayOrderAscMediaIdAsc(p)
                    .forEach(m -> {
                        if (!m.getMediaId().equals(media.getMediaId()) && m.isThumbnail()) {
                            m.setThumbnail(false);
                            mediaRepo.save(m);
                        }
                    });
        }

        return new ProductMediaResponse(
                media.getMediaId(),
                media.getUrl(),
                media.getType(),
                media.isThumbnail(),
                media.getDisplayOrder()
        );
    }

    @Override
    @Transactional
    public List<ProductMediaResponse> uploadMultiple(Long productId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No files to upload");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int startOrder = (int) mediaRepo.countByProduct(product); // tính thứ tự bắt đầu
        List<ProductMedia> toSave = new ArrayList<>();
        int idx = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "File too large (>5MB): " + file.getOriginalFilename());
            }

            String url = cloudinaryService.uploadProductMedia(file, productId);
            String ct = file.getContentType();
            String type = (ct != null && ct.startsWith("video")) ? "VIDEO" : "IMAGE";

            ProductMedia media = ProductMedia.builder()
                    .product(product)
                    .url(url)
                    .type(type)
                    .isThumbnail(false)
                    .displayOrder(startOrder + idx)
                    .build();

            toSave.add(media);
            idx++;
        }

        if (toSave.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No valid files to save");
        }

        mediaRepo.saveAll(toSave);

        // Nếu chưa có ảnh thumbnail → tự chọn ảnh đầu tiên làm thumbnail
        boolean hasThumbnail = mediaRepo.findByProduct(product).stream().anyMatch(ProductMedia::isThumbnail);
        if (!hasThumbnail) {
            ProductMedia first = toSave.get(0);
            first.setThumbnail(true);
            mediaRepo.save(first);
        }

        return toSave.stream()
                .map(m -> new ProductMediaResponse(
                        m.getMediaId(), m.getUrl(), m.getType(), m.isThumbnail(), m.getDisplayOrder()
                ))
                .toList();
    }
    @Override
    @Transactional
    public void reorder(Long productId, MediaReorderRequest req) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        List<ProductMedia> all = mediaRepo.findByProduct(product);

        if (req.orderedMediaIds() == null || req.orderedMediaIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No media IDs provided");
        }

        for (int i = 0; i < req.orderedMediaIds().size(); i++) {
            Long id = req.orderedMediaIds().get(i);
            ProductMedia media = all.stream()
                    .filter(m -> m.getMediaId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Media not found in product: " + id));

            media.setDisplayOrder(i);
        }

        mediaRepo.saveAll(all);
    }
    @Override
    @Transactional
    public void setThumbnail(Long productId, Long mediaId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        List<ProductMedia> mediaList = mediaRepo.findByProduct(product);

        if (mediaList.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No media found for this product");
        }

        // ✅ CASE 1 — Product chỉ có 1 media → tự set thumbnail
        if (mediaList.size() == 1) {
            ProductMedia only = mediaList.get(0);
            only.setThumbnail(true);
            mediaRepo.save(only);
            return;
        }

        // ✅ CASE 2 — mediaId = null → tự pick media đầu tiên để làm thumbnail
        if (mediaId == null) {
            ProductMedia first = mediaList.get(0);
            for (ProductMedia m : mediaList) {
                m.setThumbnail(m.getMediaId().equals(first.getMediaId()));
            }
            mediaRepo.saveAll(mediaList);
            return;
        }

        // ✅ CASE 3 — mediaId hợp lệ → set đúng cái đó làm thumbnail
        boolean found = false;
        for (ProductMedia m : mediaList) {
            boolean isThumb = m.getMediaId().equals(mediaId);
            m.setThumbnail(isThumb);
            if (isThumb) found = true;
        }

        if (!found) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Media not found for this product");
        }

        mediaRepo.saveAll(mediaList);
    }


}
