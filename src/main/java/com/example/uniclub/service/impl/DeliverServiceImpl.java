package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.EventStaffStateEnum;
import com.example.uniclub.enums.ProductTxStatusEnum;
import com.example.uniclub.enums.RedeemStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.DeliverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class DeliverServiceImpl implements DeliverService {

    private final RedeemRepository redeemRepository;
    private final ProductRepository productRepository;
    private final ProductTransactionRepository productTxRepository;
    private final EventStaffRepository eventStaffRepository;
    private final MembershipRepository membershipRepo;

    @Override
    @Transactional
    public void deliver(Long staffMembershipId, Long redeemId) {
        Redeem redeem = redeemRepository.findByRedeemIdAndStatus(redeemId, RedeemStatusEnum.PENDING)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Redeem not found or already processed"));

        Event event = redeem.getEvent();

        if (event != null) {

            // 🔹 CHECK: Staff phải ACTIVE trong event này
            EventStaff staff = eventStaffRepository
                    .findActiveByEventAndMembership(event.getEventId(), staffMembershipId)
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not assigned as ACTIVE staff of this event"));

            // 🔹 CHECK: Event đã hết hạn chưa (dựa trên EventDay)
            LocalDateTime eventEnd = getEventEnd(event);   // <— dùng helper mới
            LocalDateTime now = LocalDateTime.now();

            if (eventEnd == null || now.isAfter(eventEnd)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event already ended or staff expired");
            }

            if (staff.getState() != EventStaffStateEnum.ACTIVE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Your staff assignment is no longer active");
            }
        }

        // ================================
        // 🔹 Handle product stock
        // ================================

        Product product = redeem.getProduct();
        if (product.getStockQuantity() < redeem.getQuantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough stock");

        product.setStockQuantity(product.getStockQuantity() - redeem.getQuantity());
        productRepository.save(product);

        // ================================
        // 🔹 Create product transaction
        // ================================

        ProductTransaction tx = ProductTransaction.builder()
                .product(product)
                .redeem(redeem)
                .quantity(redeem.getQuantity())
                .status(ProductTxStatusEnum.DELIVERED)
                .build();
        productTxRepository.save(tx);

        // ================================
        // 🔹 Update redeem status
        // ================================

        redeem.setStatus(RedeemStatusEnum.DELIVERED);
        redeem.setDeliveredAt(LocalDateTime.now());

        // ⭐ Không tạo membership mới — dùng reference
        if (event != null) {
            Membership staffRef = membershipRepo.getReferenceById(staffMembershipId);
            redeem.setStaff(staffRef);
        }

        redeemRepository.save(redeem);
    }

    public LocalDateTime getEventStart(Event event) {
        if (event.getDays() == null || event.getDays().isEmpty()) return null;

        EventDay earliest = event.getDays().stream()
                .min(Comparator.comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .orElse(null);

        return (earliest == null) ? null :
                LocalDateTime.of(earliest.getDate(), earliest.getStartTime());
    }

    public LocalDateTime getEventEnd(Event event) {
        if (event.getDays() == null || event.getDays().isEmpty()) return null;

        EventDay latest = event.getDays().stream()
                .max(Comparator.comparing(EventDay::getDate)
                        .thenComparing(EventDay::getEndTime))
                .orElse(null);

        return (latest == null) ? null :
                LocalDateTime.of(latest.getDate(), latest.getEndTime());
    }


}
