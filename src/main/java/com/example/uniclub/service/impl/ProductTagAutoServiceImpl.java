package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Product;
import com.example.uniclub.entity.ProductTag;
import com.example.uniclub.entity.Tag;
import com.example.uniclub.repository.ProductRepository;
import com.example.uniclub.repository.ProductTagRepository;
import com.example.uniclub.repository.TagRepository;
import com.example.uniclub.service.ProductTagAutoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductTagAutoServiceImpl implements ProductTagAutoService {

    private final ProductRepository productRepo;
    private final ProductTagRepository productTagRepo;
    private final TagRepository tagRepo;

    @Override
    @Transactional
    public void updateDynamicTags() {
        // 🔹 Load tag động
        Tag tagNew = tagRepo.findByNameIgnoreCase("new").orElse(null);
        Tag tagHot = tagRepo.findByNameIgnoreCase("hot").orElse(null);
        Tag tagLimited = tagRepo.findByNameIgnoreCase("limited").orElse(null);
        Tag tagBestSeller = tagRepo.findByNameIgnoreCase("best_seller").orElse(null);
        Tag tagExclusive = tagRepo.findByNameIgnoreCase("exclusive").orElse(null);

        if (tagNew == null || tagHot == null || tagLimited == null) {
            log.warn("⚠️ Missing required tags (new/hot/limited). Skipping auto-tagging...");
            return;
        }

        var products = productRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Product p : products) {
            boolean isNew = p.getCreatedAt() != null && p.getCreatedAt().isAfter(now.minusDays(7));
            boolean isHot = p.getRedeemCount() != null && p.getRedeemCount() >= 50;
            boolean isLimited = p.getStockQuantity() != null && p.getStockQuantity() < 10;
            boolean isBestSeller = p.getRedeemCount() != null && p.getRedeemCount() >= 200;
            boolean isExclusive = p.getPointCost() != null && p.getPointCost() >= 1000; // tùy theo mức điểm cao

            handleTag(p, tagNew, isNew);
            handleTag(p, tagHot, isHot);
            handleTag(p, tagLimited, isLimited);
            handleTag(p, tagBestSeller, isBestSeller);
            handleTag(p, tagExclusive, isExclusive);
        }

        log.info("✅ Auto-tagging completed successfully for {} products.", products.size());
    }


    private void handleTag(Product p, Tag tag, boolean shouldHaveTag) {
        boolean hasTag = p.getProductTags().stream()
                .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase(tag.getName()));

        if (shouldHaveTag && !hasTag) {
            productTagRepo.save(ProductTag.builder()
                    .product(p)
                    .tag(tag)
                    .build());
            log.info("➕ Added tag [{}] to product {}", tag.getName(), p.getName());

        } else if (!shouldHaveTag && hasTag) {
            // ⚙️ Cách xóa linh hoạt tuỳ repo bạn có
            try {
                productTagRepo.deleteByProductAndTag(p, tag);
            } catch (Exception e) {
                // Nếu repo bạn chỉ có deleteByProductIdAndTagIds(...)
                productTagRepo.deleteByProductIdAndTagIds(p.getProductId(), Set.of(tag.getTagId()));
            }
            log.info("❌ Removed tag [{}] from product {}", tag.getName(), p.getName());
        }
    }
}
