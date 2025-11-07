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
        Tag tagNew = tagRepo.findByNameIgnoreCase("new").orElse(null);
        Tag tagHot = tagRepo.findByNameIgnoreCase("hot").orElse(null);
        Tag tagLimited = tagRepo.findByNameIgnoreCase("limited").orElse(null);

        if (tagNew == null || tagHot == null || tagLimited == null) {
            log.warn(" Missing required tags (new/hot/limited). Auto-tagging skipped.");
            return;
        }

        var products = productRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Product p : products) {
            boolean isNew = p.getCreatedAt() != null && p.getCreatedAt().isAfter(now.minusDays(7));
            boolean isHot = p.getRedeemCount() != null && p.getRedeemCount() >= 50;
            boolean isLimited = p.getStockQuantity() != null && p.getStockQuantity() < 10;

            handleTag(p, tagNew, isNew);
            handleTag(p, tagHot, isHot);
            handleTag(p, tagLimited, isLimited);
        }

        log.info(" Auto-tagging completed successfully at {}", now);
    }

    private void handleTag(Product p, Tag tag, boolean shouldHaveTag) {
        boolean hasTag = p.getProductTags().stream()
                .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase(tag.getName()));

        if (shouldHaveTag && !hasTag) {
            productTagRepo.save(ProductTag.builder().product(p).tag(tag).build());
        } else if (!shouldHaveTag && hasTag) {
            productTagRepo.deleteByProductAndTag(p, tag);
        }
    }
}
