package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.request.ProductUpdateRequest;
import com.example.uniclub.dto.response.EventProductResponse;
import com.example.uniclub.dto.response.EventValidityResponse;
import com.example.uniclub.dto.response.ProductMediaResponse;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.ProductStatusEnum;
import com.example.uniclub.enums.ProductTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.uniclub.repository.TagRepository;
import com.example.uniclub.repository.ProductTagRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final TagRepository tagRepo;
    private final ProductTagRepository productTagRepo;

    private final TagRepository tagRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductMediaRepository mediaRepo;
    private final ProductStockHistoryRepository stockHistoryRepo;
    private List<ProductMedia> mediaList;
    private List<ProductTag> productTags;

    // =========================
    // Mapper
    // =========================
    private ProductResponse toResp(Product p) {
        var media = p.getMediaList() == null ? List.<ProductMediaResponse>of()
                : p.getMediaList().stream()
                .sorted(Comparator.comparingInt(ProductMedia::getDisplayOrder)
                        .thenComparing(ProductMedia::getMediaId))
                .map(m -> new ProductMediaResponse(
                        m.getMediaId(), m.getUrl(), m.getType(), m.isThumbnail(), m.getDisplayOrder()
                )).toList();

        var tags = p.getProductTags() == null ? List.<String>of()
                : p.getProductTags().stream().map(pt -> pt.getTag().getName()).toList();

        return new ProductResponse(
                p.getProductId(),
                p.getProductCode(),
                p.getName(),
                p.getDescription(),
                p.getPointCost(),
                p.getStockQuantity(),
                p.getType().name(),
                p.getStatus().name(),
                p.getClub() != null ? p.getClub().getClubId() : null,
                p.getClub() != null ? p.getClub().getName() : null,
                p.getEvent() != null ? p.getEvent().getEventId() : null,
                p.getEvent() != null ? p.getEvent().getStatus().name() : null,
                p.getCreatedAt(),
                p.getRedeemCount(),
                media,
                tags
        );
    }

    private void archiveIfExpiredEventItem(Product p) {
        if (p.getType() == ProductTypeEnum.EVENT_ITEM && p.getEvent() != null) {
            LocalDate today = LocalDate.now();
            if (p.getEvent().getDate() != null && p.getEvent().getDate().isBefore(today)) {
                if (p.getStatus() != ProductStatusEnum.ARCHIVED) {
                    p.setStatus(ProductStatusEnum.ARCHIVED);
                    productRepo.save(p);
                }
            }
        }
    }

    // =========================
    // CREATE
    // =========================
    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest req, Long clubId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        Event event = null;
        if (req.type() == ProductTypeEnum.EVENT_ITEM) {
            if (req.eventId() == null)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event ID is required for EVENT_ITEM");

            event = eventRepo.findById(req.eventId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

            if (!Objects.equals(event.getHostClub().getClubId(), club.getClubId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event does not belong to this club");
            }

            LocalDate today = LocalDate.now();
            if (event.getDate() != null && event.getDate().isBefore(today)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event has already ended. Cannot create EVENT_ITEM product.");
            }
        }

        // ✅ Kiểm tra trùng tên
        Optional<Product> opt = productRepo.findByClubAndNameIgnoreCase(club, req.name());
        if (opt.isPresent()) {
            Product existing = opt.get();
            if (existing.getStatus() == ProductStatusEnum.INACTIVE) {
                existing.setStatus(ProductStatusEnum.ACTIVE);
                existing.setIsActive(true);
                existing.setDescription(req.description());
                existing.setPointCost(req.pointCost());
                existing.setStockQuantity(req.stockQuantity());
                existing.setType(req.type());
                existing.setEvent(event);
                productRepo.save(existing);
                if (req.tagIds() != null) syncTags(existing, req.tagIds());
                throw new ApiException(HttpStatus.OK, "REACTIVATED:" + existing.getProductId());
            }
        }

        // ✅ Tạo mới sản phẩm
        Product p = Product.builder()
                .club(club)
                .event(event)
                .name(req.name())
                .description(req.description())
                .pointCost(req.pointCost())
                .stockQuantity(req.stockQuantity())
                .type(req.type())
                .status(ProductStatusEnum.ACTIVE)
                .isActive(true)
                .build();

        p = productRepo.save(p);

        // ✅ Gắn tag "new" và "limited" ngay khi tạo
        try {
            final Product product = p;

            // 🆕 Tag NEW (sản phẩm mới)
            tagRepo.findByNameIgnoreCase("new").ifPresent(tagNew -> {
                boolean alreadyTagged = product.getProductTags().stream()
                        .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase("new"));
                if (!alreadyTagged) {
                    ProductTag newTag = ProductTag.builder()
                            .product(product)
                            .tag(tagNew)
                            .build();
                    productTagRepo.save(newTag);
                    log.info("🆕 Added tag [new] for product {}", product.getName());
                }
            });

            // ⚠️ Tag LIMITED (khi số lượng < 10)
            if (product.getStockQuantity() != null && product.getStockQuantity() < 10) {
                tagRepo.findByNameIgnoreCase("limited").ifPresent(tagLimited -> {
                    boolean alreadyTagged = product.getProductTags().stream()
                            .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase("limited"));
                    if (!alreadyTagged) {
                        ProductTag limitedTag = ProductTag.builder()
                                .product(product)
                                .tag(tagLimited)
                                .build();
                        productTagRepo.save(limitedTag);
                        log.info("⚠️ Added tag [limited] for product {}", product.getName());
                    }
                });
            }

        } catch (Exception e) {
            log.error("⚠️ Failed to auto-tag product {}: {}", p.getName(), e.getMessage());
        }

        // ✅ Gắn thêm tag người dùng chọn thủ công (nếu có)
        if (req.tagIds() != null) syncTags(p, req.tagIds());

        return toResp(p);
    }




    private void syncTags(Product product, List<Long> tagIds) {
        List<Long> inputTagIds = (tagIds == null) ? List.of() : tagIds;

        Set<Long> currentTagIds = product.getProductTags()
                .stream()
                .map(pt -> pt.getTag().getTagId())
                .collect(Collectors.toSet());

        Set<Long> tagsToRemove = currentTagIds.stream()
                .filter(id -> !inputTagIds.contains(id))
                .collect(Collectors.toSet());

        Set<Long> tagsToAdd = inputTagIds.stream()
                .filter(id -> !currentTagIds.contains(id))
                .collect(Collectors.toSet());

        // 1️⃣ Remove tags
        if (!tagsToRemove.isEmpty()) {
            product.getProductTags().removeIf(pt -> tagsToRemove.contains(pt.getTag().getTagId()));
            productTagRepository.deleteByProductIdAndTagIds(product.getProductId(), tagsToRemove);
        }

        // 2️⃣ Add tags
        if (!tagsToAdd.isEmpty()) {
            List<Tag> newTags = tagRepository.findAllById(tagsToAdd);
            List<ProductTag> newProductTags = newTags.stream()
                    .map(tag -> ProductTag.builder()
                            .product(product)
                            .tag(tag)
                            .build())
                    .toList();

            product.getProductTags().addAll(newProductTags);
            productTagRepository.saveAll(newProductTags);
        }
    }

    // =========================
    // GET
    // =========================
    @Override
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        archiveIfExpiredEventItem(p);
        return toResp(p);
    }

    // =========================
    // ADMIN FILTER LIST
    // =========================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> adminFilterList(Pageable pageable, String status, String type, String tag, String keyword) {
        List<Product> all = productRepo.findAll();

        List<ProductResponse> filtered = all.stream()
                .peek(this::archiveIfExpiredEventItem)
                .filter(p -> {
                    if (status != null && !status.isBlank()) {
                        try {
                            ProductStatusEnum s = ProductStatusEnum.valueOf(status.toUpperCase());
                            if (p.getStatus() != s) return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    if (type != null && !type.isBlank()) {
                        try {
                            ProductTypeEnum t = ProductTypeEnum.valueOf(type.toUpperCase());
                            if (p.getType() != t) return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    if (tag != null && !tag.isBlank()) {
                        return p.getProductTags().stream()
                                .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase(tag));
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.toLowerCase();
                        return (p.getName() != null && p.getName().toLowerCase().contains(kw))
                                || (p.getDescription() != null && p.getDescription().toLowerCase().contains(kw));
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                .map(this::toResp)
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listByClub(Long clubId, Pageable pageable, Boolean includeInactive, Boolean includeArchived) {
        List<Product> all = productRepo.findAll().stream()
                .filter(p -> p.getClub() != null && Objects.equals(p.getClub().getClubId(), clubId))
                .toList();

        List<ProductResponse> filtered = all.stream()
                .peek(this::archiveIfExpiredEventItem)
                .filter(p -> {
                    if (!includeArchived && p.getStatus() == ProductStatusEnum.ARCHIVED) return false;
                    if (!includeInactive && p.getStatus() == ProductStatusEnum.INACTIVE) return false;
                    return true;
                })
                .map(this::toResp)
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> listByClub(Long clubId, boolean includeInactive, boolean includeArchived) {
        List<Product> all = productRepo.findAll().stream()
                .filter(p -> p.getClub() != null && Objects.equals(p.getClub().getClubId(), clubId))
                .toList();

        return all.stream()
                .peek(this::archiveIfExpiredEventItem)
                .filter(p -> {
                    if (!includeArchived && p.getStatus() == ProductStatusEnum.ARCHIVED) return false;
                    if (!includeInactive && p.getStatus() == ProductStatusEnum.INACTIVE) return false;
                    return true;
                })
                .map(this::toResp)
                .toList();
    }

    // =========================
    // UPDATE
    // =========================
    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (p.getStatus() == ProductStatusEnum.ARCHIVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot edit archived product");
        }

        if (req.name() != null && !req.name().isBlank()) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.pointCost() != null && req.pointCost() >= 0) p.setPointCost(req.pointCost());
        if (req.stockQuantity() != null && req.stockQuantity() >= 0) p.setStockQuantity(req.stockQuantity());

        if (req.type() != null) {
            p.setType(req.type());
            if (req.type() == ProductTypeEnum.EVENT_ITEM) {
                if (req.eventId() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Event ID is required for EVENT_ITEM");
                Event ev = eventRepo.findById(req.eventId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

                if (!Objects.equals(ev.getHostClub().getClubId(), p.getClub().getClubId()))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Event does not belong to this club");

                if (ev.getDate() != null && ev.getDate().isBefore(LocalDate.now()))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Event has already ended and cannot be assigned");

                p.setEvent(ev);
            } else {
                p.setEvent(null);
            }
        }

        if (req.status() != null) {
            p.setStatus(req.status());
            p.setIsActive(p.getStatus() != ProductStatusEnum.ARCHIVED);
        }

        productRepo.save(p);

        if (req.tagIds() != null) {
            syncTags(p, req.tagIds());
        }

        archiveIfExpiredEventItem(p);
        return toResp(p);
    }

    // =========================
    // STOCK & HISTORY
    // =========================
    @Override
    @Transactional
    public ProductResponse updateStock(Long id, Integer delta, String note) {
        if (delta == null || delta == 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid stock adjustment amount");

        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int old = p.getStockQuantity();
        int newStock = old + delta;
        if (newStock < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");

        p.setStockQuantity(newStock);
        productRepo.save(p);

        ProductStockHistory log = ProductStockHistory.builder()
                .product(p)
                .oldStock(old)
                .newStock(newStock)
                .note(note == null ? (delta > 0 ? "Import" : "Adjust") : note)
                .build();
        stockHistoryRepo.save(log);
        return toResp(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockHistory> getStockHistory(Long productId) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        return stockHistoryRepo.findByProductOrderByChangedAtDesc(p);
    }

    // =========================
    // DELETE
    // =========================
    @Override
    @Transactional
    public void delete(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        p.setStatus(ProductStatusEnum.INACTIVE);
        p.setIsActive(false);
        productRepo.save(p);
    }

    // =========================
    // SEARCH BY TAG
    // =========================
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchByTags(List<String> tagNames) {
        List<Product> all = productRepo.findAll();
        if (tagNames == null || tagNames.isEmpty()) {
            return all.stream().peek(this::archiveIfExpiredEventItem)
                    .filter(p -> p.getStatus() == ProductStatusEnum.ACTIVE)
                    .map(this::toResp).toList();
        }

        Set<String> q = tagNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
        return all.stream()
                .peek(this::archiveIfExpiredEventItem)
                .filter(p -> p.getStatus() == ProductStatusEnum.ACTIVE)
                .filter(p -> p.getProductTags() != null && p.getProductTags().stream()
                        .anyMatch(pt -> q.contains(pt.getTag().getName().toLowerCase())))
                .map(this::toResp)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(Pageable pageable) {
        return productRepo.findAll(pageable)
                .map(p -> {
                    archiveIfExpiredEventItem(p);
                    return toResp(p);
                });
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long clubId, Long productId, ProductUpdateRequest req) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Product does not belong to this club");
        }

        if (req.name() != null && !req.name().isBlank()) product.setName(req.name());
        if (req.description() != null) product.setDescription(req.description());
        if (req.pointCost() != null && req.pointCost() >= 0) product.setPointCost(req.pointCost());
        if (req.stockQuantity() != null && req.stockQuantity() >= 0) product.setStockQuantity(req.stockQuantity());
        if (req.status() != null) product.setStatus(req.status());

        if (req.type() != null && req.type() != product.getType()) {
            if (req.type() == ProductTypeEnum.EVENT_ITEM) {
                if (req.eventId() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Event ID required for EVENT_ITEM");
                Event event = eventRepo.findById(req.eventId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
                product.setEvent(event);
            } else if (req.type() == ProductTypeEnum.CLUB_ITEM) {
                product.setEvent(null);
            }
            product.setType(req.type());
        }

        if (req.tagIds() != null) {
            product.getProductTags().clear();
            List<ProductTag> newTags = req.tagIds().stream()
                    .map(tagId -> ProductTag.builder()
                            .product(product)
                            .tag(Tag.builder().tagId(tagId).build())
                            .build())
                    .toList();
            product.getProductTags().addAll(newTags);
        }

        productRepo.save(product);
        return toResp(product);
    }
    @Override
    public EventValidityResponse checkEventValidity(Long productId) {

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        // ❗ Chỉ áp dụng cho EVENT_ITEM
        if (p.getType() != ProductTypeEnum.EVENT_ITEM) {
            return EventValidityResponse.notEventProduct(p.getProductId());
        }

        // ❗ Sản phẩm EVENT_ITEM nhưng không có event
        if (p.getEvent() == null) {
            return EventValidityResponse.noEventLinked(p.getProductId());
        }

        Event ev = p.getEvent();

        // Tính thời điểm kết thúc sự kiện
        LocalDateTime eventEnd = LocalDateTime.of(ev.getDate(), ev.getEndTime());
        LocalDateTime now = LocalDateTime.now();

        boolean expired =
                ev.getStatus() == EventStatusEnum.COMPLETED ||
                        eventEnd.isBefore(now);

        return EventValidityResponse.ok(
                p.getProductId(),
                ev.getEventId(),
                ev.getStatus(),
                expired,
                eventEnd
        );
    }

    @Override
    @Transactional
    public ProductResponse activateProduct(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getStatus() == ProductStatusEnum.ARCHIVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Archived products cannot be reactivated");
        }

        if (product.getStatus() == ProductStatusEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is already active");
        }

        product.setStatus(ProductStatusEnum.ACTIVE);
        product.setIsActive(true);
        productRepo.save(product);

        return new ProductResponse(
                product.getProductId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getPointCost(),
                product.getStockQuantity(),
                product.getType().name(),
                product.getStatus().name(),
                product.getClub().getClubId(),
                product.getClub().getName(),
                product.getEvent() != null ? product.getEvent().getEventId() : null,
                product.getEvent() != null ? product.getEvent().getStatus().name() : null,
                product.getCreatedAt(),
                product.getRedeemCount(),
                product.getMediaList() != null
                        ? product.getMediaList().stream()
                        .map(ProductMediaResponse::fromEntity)
                        .toList()
                        : List.of(),
                product.getProductTags() != null
                        ? product.getProductTags().stream()
                        .map(tag -> tag.getTag().getName())
                        .toList()
                        : List.of()
        );
    }
    @Override
    public List<EventProductResponse> listEventProductsByClub(Long clubId) {

        List<Product> list = productRepo.findByClub_ClubIdAndType(clubId, ProductTypeEnum.EVENT_ITEM);

        LocalDateTime now = LocalDateTime.now();

        return list.stream().map(p -> {

            Event e = p.getEvent();

            LocalDateTime eventEnd = LocalDateTime.of(
                    e.getDate(),
                    e.getEndTime()
            );

            boolean expired =
                    e.getStatus() == EventStatusEnum.COMPLETED ||
                            eventEnd.isBefore(now);

            return EventProductResponse.builder()
                    .productId(p.getProductId())
                    .name(p.getName())
                    .pointCost(p.getPointCost())
                    .eventId(e.getEventId())
                    .eventName(e.getName())
                    .eventStatus(e.getStatus())
                    .expired(expired)
                    .build();

        }).toList();
    }
    @Override
    public List<EventProductResponse> listEventProductsByClubAndStatuses(Long clubId, List<EventStatusEnum> statuses) {

        List<Product> list = productRepo.findByClubClubIdAndType(clubId, ProductTypeEnum.EVENT_ITEM);

        LocalDateTime now = LocalDateTime.now();

        return list.stream()
                .filter(p -> p.getEvent() != null && statuses.contains(p.getEvent().getStatus()))
                .map(p -> {
                    Event e = p.getEvent();
                    LocalDateTime eventEnd = LocalDateTime.of(e.getDate(), e.getEndTime());

                    boolean expired = (e.getStatus() == EventStatusEnum.COMPLETED || eventEnd.isBefore(now));

                    return EventProductResponse.builder()
                            .productId(p.getProductId())
                            .name(p.getName())
                            .pointCost(p.getPointCost())
                            .eventId(e.getEventId())
                            .eventName(e.getName())
                            .eventStatus(e.getStatus())
                            .expired(expired)
                            .build();
                })
                .toList();
    }

}
