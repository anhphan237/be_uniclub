package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.request.ProductUpdateRequest;
import com.example.uniclub.dto.response.ProductMediaResponse;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ProductStatusEnum;
import com.example.uniclub.enums.ProductTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;

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
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event không thuộc CLB này");
            }

            LocalDate today = LocalDate.now();
            if (event.getDate() != null && event.getDate().isBefore(today)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event đã kết thúc, không thể tạo sản phẩm EVENT_ITEM");
            }
        }

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
        if (req.tagIds() != null) syncTags(p, req.tagIds());
        return toResp(p);
    }

    private void syncTags(Product product, List<Long> tagIds) {
        // Nếu null thì coi như empty list
        List<Long> inputTagIds = (tagIds == null) ? List.of() : tagIds;

        // Lấy danh sách liên kết hiện tại
        Set<Long> currentTagIds = product.getProductTags()
                .stream()
                .map(pt -> pt.getTag().getTagId())
                .collect(Collectors.toSet());

        // Tag cần xóa = tag hiện tại mà không nằm trong request
        Set<Long> tagsToRemove = currentTagIds.stream()
                .filter(id -> !inputTagIds.contains(id))
                .collect(Collectors.toSet());

        // Tag cần thêm = tag trong request nhưng chưa có
        Set<Long> tagsToAdd = inputTagIds.stream()
                .filter(id -> !currentTagIds.contains(id))
                .collect(Collectors.toSet());

        // 1️⃣ Xóa
        if (!tagsToRemove.isEmpty()) {
            product.getProductTags().removeIf(pt -> tagsToRemove.contains(pt.getTag().getTagId()));
            productTagRepository.deleteByProductIdAndTagIds(product.getProductId(), tagsToRemove);
        }

        // 2️⃣ Thêm
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
        archiveIfExpiredEventItem(p);
        return toResp(p);
    }

    // =========================
    // ADMIN FILTER LIST (NEW)
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

        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
    }

    // =========================
    // LIST BY CLUB (KHÔNG PHÂN TRANG)
    // =========================
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));

        if (p.getStatus() == ProductStatusEnum.INACTIVE)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể chỉnh sửa sản phẩm đã INACTIVE");

        if (req.name() != null && !req.name().isBlank()) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.pointCost() != null && req.pointCost() >= 0) p.setPointCost(req.pointCost());
        if (req.stockQuantity() != null && req.stockQuantity() >= 0) p.setStockQuantity(req.stockQuantity());

        if (req.type() != null) {
            p.setType(req.type());
            if (req.type() == ProductTypeEnum.EVENT_ITEM) {
                if (req.eventId() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "EVENT_ITEM cần eventId");
                Event ev = eventRepo.findById(req.eventId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
                if (!Objects.equals(ev.getHostClub().getClubId(), p.getClub().getClubId()))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Event không thuộc CLB của sản phẩm");
                if (ev.getDate() != null && ev.getDate().isBefore(LocalDate.now()))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Event đã kết thúc, không thể gán");
                p.setEvent(ev);
            } else {
                p.setEvent(null);
            }
        }

        if (req.status() != null) {
            p.setStatus(req.status());
            p.setIsActive(req.status() == ProductStatusEnum.ACTIVE);
        }

        productRepo.save(p);
        if (req.tagIds() != null) syncTags(p, req.tagIds());
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "Số lượng cập nhật không hợp lệ");

        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));

        int old = p.getStockQuantity();
        int newStock = old + delta;
        if (newStock < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Stock không thể âm");

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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
        return stockHistoryRepo.findByProductOrderByChangedAtDesc(p);
    }

    // =========================
    // DELETE
    // =========================
    @Override
    @Transactional
    public void delete(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
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

        // 🧩 Cập nhật cơ bản
        if (req.name() != null && !req.name().isBlank()) product.setName(req.name());
        if (req.description() != null) product.setDescription(req.description());
        if (req.pointCost() != null && req.pointCost() >= 0) product.setPointCost(req.pointCost());
        if (req.stockQuantity() != null && req.stockQuantity() >= 0) product.setStockQuantity(req.stockQuantity());
        if (req.status() != null) product.setStatus(req.status());

        // 🧩 Nếu có type mới — chỉ cho phép đổi nếu là CLUB_ITEM ↔ EVENT_ITEM hợp lệ
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

        // 🏷️ Nếu có tagIds mới thì cập nhật lại (xoá hết tag cũ trước)
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

        // 🔁 Lưu lại
        productRepo.save(product);

        return toResp(product);
    }
    @Override
    @Transactional
    public ProductResponse activateProduct(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        // Không cho bật lại ARCHIVED
        if (product.getStatus() == ProductStatusEnum.ARCHIVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Archived products cannot be reactivated");
        }

        // Nếu đã ACTIVE thì bỏ qua
        if (product.getStatus() == ProductStatusEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is already active");
        }

        // Cập nhật trạng thái
        product.setStatus(ProductStatusEnum.ACTIVE);
        product.setIsActive(true);
        productRepo.save(product);

        // Trả về DTO đầy đủ
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


}
