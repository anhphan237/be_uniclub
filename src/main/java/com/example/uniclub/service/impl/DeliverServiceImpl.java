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

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DeliverServiceImpl implements DeliverService {

    private final RedeemRepository redeemRepository;
    private final ProductRepository productRepository;
    private final ProductTransactionRepository productTxRepository;
    private final EventStaffRepository eventStaffRepository;

    @Override
    @Transactional
    public void deliver(Long staffMembershipId, Long redeemId) {
        Redeem redeem = redeemRepository.findByRedeemIdAndStatus(redeemId, RedeemStatusEnum.PENDING)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Redeem not found or already processed"));

        Event event = redeem.getEvent();
        if (event != null) {
            EventStaff staff = eventStaffRepository
                    .findActiveByEventAndMembership(event.getEventId(), staffMembershipId)
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not assigned as ACTIVE staff of this event"));

            boolean ended = event.getDate().isBefore(LocalDate.now())
                    || (event.getDate().isEqual(LocalDate.now()) && event.getEndTime().isBefore(LocalTime.now()));
            if (ended || staff.getState() != EventStaffStateEnum.ACTIVE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Event already ended or staff expired");
            }
        }

        Product product = redeem.getProduct();
        if (product.getStockQuantity() < redeem.getQuantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough stock");

        product.setStockQuantity(product.getStockQuantity() - redeem.getQuantity());
        productRepository.save(product);

        ProductTransaction tx = ProductTransaction.builder()
                .product(product)
                .redeem(redeem)
                .quantity(redeem.getQuantity())
                .status(ProductTxStatusEnum.DELIVERED)
                .build();
        productTxRepository.save(tx);

        redeem.setStatus(RedeemStatusEnum.DELIVERED);
        redeem.setDeliveredAt(java.time.LocalDateTime.now());
        if (redeem.getEvent() != null) {
            redeem.setStaff(Membership.builder().membershipId(staffMembershipId).build());
        }
        redeemRepository.save(redeem);
    }
}
