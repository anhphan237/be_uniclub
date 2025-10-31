package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Product;
import com.example.uniclub.enums.ProductTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.ProductRepository;
import com.example.uniclub.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;

    // 🧩 Mapper: Product → ProductResponse
    private ProductResponse toResp(Product p) {
        List<ProductResponse.MediaItem> mediaItems = (p.getMediaList() != null)
                ? p.getMediaList().stream()
                .map(m -> new ProductResponse.MediaItem(
                        m.getMediaId(),
                        m.getUrl(),
                        m.getType(),
                        m.isThumbnail(),
                        m.getDisplayOrder()))
                .toList()
                : List.of();

        return new ProductResponse(
                p.getProductId(),
                p.getName(),
                p.getDescription(),
                p.getPointCost(),
                p.getStockQuantity(),
                p.getType().name(),
                p.getClub() != null ? p.getClub().getClubId() : null,
                p.getEvent() != null ? p.getEvent().getEventId() : null,
                p.getIsActive(),
                mediaItems
        );
    }


    @Override
    public ProductResponse create(ProductCreateRequest req, Long clubId) {
        // 🔸 Kiểm tra Club tồn tại
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // 🔸 Nếu là EVENT_ITEM, kiểm tra event
        Event event = null;
        if (req.type() == ProductTypeEnum.EVENT_ITEM) {
            if (req.eventId() == null)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event ID is required for EVENT_ITEM");
            event = eventRepo.findById(req.eventId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        }

        Product p = Product.builder()
                .club(club)
                .event(event)
                .name(req.name())
                .description(req.description())
                .pointCost(req.pointCost())
                .stockQuantity(req.stockQuantity())
                .type(req.type())
                .isActive(true)
                .build();

        productRepo.save(p);
        return toResp(p);
    }

    @Override
    public ProductResponse get(Long id) {
        return productRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
    }

    @Override
    public Page<ProductResponse> list(Pageable pageable) {
        return productRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public ProductResponse updateStock(Long id, Integer stock) {
        var p = productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại"));
        if (stock == null || stock < 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stock không hợp lệ");
        p.setStockQuantity(stock);
        productRepo.save(p);
        return toResp(p);
    }

    @Override
    public void delete(Long id) {
        if (!productRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Product không tồn tại");
        productRepo.deleteById(id);
    }
}
