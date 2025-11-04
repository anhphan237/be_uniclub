package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.QrService;
import com.example.uniclub.service.RedeemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedeemServiceImpl implements RedeemService {

    private final ProductRepository productRepo;
    private final ProductOrderRepository orderRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final MembershipRepository membershipRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final UserRepository userRepo;
    private final QrService qrService;
    private final EmailService emailService;

    private OrderResponse toResponse(ProductOrder o) {
        return new OrderResponse(
                o.getOrderId(),
                o.getOrderCode(),
                o.getProduct().getName(),
                o.getQuantity(),
                o.getTotalPoints(),
                o.getStatus().name(),
                o.getCreatedAt(),
                o.getCompletedAt(),
                o.getClub().getName(),
                o.getMembership().getUser().getFullName()
        );
    }

    // 🟢 Thành viên đổi hàng trong CLB
    @Override
    @Transactional
    public OrderResponse createClubOrder(Long clubId, RedeemOrderRequest req, Long userId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.getClub().getClubId().equals(clubId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product not belongs to this club");

        if (!Boolean.TRUE.equals(product.getIsActive()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is inactive");

        if (product.getType() != ProductTypeEnum.CLUB_ITEM)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not CLUB_ITEM");

        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubIdAndState(userId, clubId, MembershipStateEnum.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not active member of this club"));

        if (product.getStockQuantity() < req.quantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Out of stock");

        long totalPoints = product.getPointCost() * req.quantity(); // ✅ Long-safe

        Optional<Club> existClub = clubRepo.findByClubId(product.getProductId());
        Wallet wallet = walletRepo
                .findByUser_UserIdAndClub_ClubId(userId, existClub.get().getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found for membership"));

        if (wallet.getBalancePoints() < totalPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        wallet.setBalancePoints(wallet.getBalancePoints() - totalPoints);
        product.setStockQuantity(product.getStockQuantity() - req.quantity());

        ProductOrder order = ProductOrder.builder()
                .product(product)
                .membership(membership)
                .club(club)
                .quantity(req.quantity())
                .totalPoints(totalPoints)
                .status(OrderStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(-1L * totalPoints)
                .type(WalletTransactionTypeEnum.REDEEM_PRODUCT)
                .description("Redeem product: " + product.getName())
                .build();

        walletRepo.save(wallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(tx);

        // 🧾 Gán mã đơn & QR
        String orderCode = "UC-" + Long.toHexString(order.getOrderId()).toUpperCase();
        String qrBase64 = qrService.generateQrAsBase64(orderCode);
        order.setOrderCode(orderCode);
        order.setQrCodeBase64(qrBase64);
        orderRepo.save(order);

        // 📧 Gửi email thông báo
        String memberEmail = membership.getUser().getEmail();
        String content = """
            <h3>🎉 Bạn đã đổi hàng thành công!</h3>
            <p><b>Sản phẩm:</b> %s</p>
            <p><b>Số lượng:</b> %d</p>
            <p><b>Điểm đã trừ:</b> %d</p>
            <p><b>Mã đơn hàng:</b> %s</p>
            <div style='text-align:center;margin:20px 0'>
                <img src="data:image/png;base64,%s" alt="QR Code" style="width:150px"/>
            </div>
            """.formatted(product.getName(), req.quantity(), totalPoints, orderCode, qrBase64);

        emailService.sendEmail(memberEmail, "[UniClub] Xác nhận đổi hàng #" + orderCode, content);
        return toResponse(order);
    }

    // 🟢 Staff redeem trong event booth
    @Override
    @Transactional
    public OrderResponse eventRedeem(Long eventId, RedeemOrderRequest req, Long staffUserId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!isEventStillActive(event))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event has not started or has already ended");

        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getType() != ProductTypeEnum.EVENT_ITEM)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not EVENT_ITEM");

        if (product.getEvent() == null || !product.getEvent().getEventId().equals(eventId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product not belongs to this event");

        if (product.getStockQuantity() < req.quantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Out of stock");

        Membership membership = membershipRepo.findById(req.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        long totalPoints = product.getPointCost() * req.quantity(); // ✅ Long-safe
        Optional<Club> existClub = clubRepo.findByClubId(product.getProductId());
        Wallet wallet = walletRepo.findByUser_UserIdAndClub_ClubId(staffUserId, existClub.get().getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getBalancePoints() < totalPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        wallet.setBalancePoints(wallet.getBalancePoints() - totalPoints);
        product.setStockQuantity(product.getStockQuantity() - req.quantity());

        ProductOrder order = ProductOrder.builder()
                .product(product)
                .membership(membership)
                .club(product.getClub())
                .quantity(req.quantity())
                .totalPoints(totalPoints)
                .status(OrderStatusEnum.COMPLETED)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(-1L * totalPoints)
                .type(WalletTransactionTypeEnum.EVENT_REDEEM_PRODUCT)
                .description("Event redeem: " + product.getName())
                .build();

        walletRepo.save(wallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(tx);

        String orderCode = "EV-" + Long.toHexString(order.getOrderId()).toUpperCase();
        String qrBase64 = qrService.generateQrAsBase64(orderCode);
        order.setOrderCode(orderCode);
        order.setQrCodeBase64(qrBase64);
        orderRepo.save(order);

        String memberEmail = membership.getUser().getEmail();
        String content = """
            <h3>🎉 Đổi quà tại sự kiện thành công!</h3>
            <p><b>Sản phẩm:</b> %s</p>
            <p><b>Số lượng:</b> %d</p>
            <p><b>Điểm đã trừ:</b> %d</p>
            <p><b>Mã đơn hàng:</b> %s</p>
            """.formatted(product.getName(), req.quantity(), totalPoints, orderCode);

        emailService.sendEmail(memberEmail, "[UniClub] Đổi quà tại sự kiện " + event.getName(), content);
        return toResponse(order);
    }

    // 🟡 Hoàn hàng toàn phần
    @Override
    @Transactional
    public OrderResponse refund(Long orderId, Long staffUserId) {
        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatusEnum.REFUNDED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order already refunded");

        Product product = order.getProduct();
        Optional<Club> existClub = clubRepo.findByClubId(product.getProductId());
        Wallet wallet = walletRepo.findByUser_UserIdAndClub_ClubId(staffUserId, existClub.get().getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        wallet.setBalancePoints(wallet.getBalancePoints() + order.getTotalPoints());
        product.setStockQuantity(product.getStockQuantity() + order.getQuantity());

        order.setStatus(OrderStatusEnum.REFUNDED);
        order.setCompletedAt(LocalDateTime.now());

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(order.getTotalPoints())
                .type(WalletTransactionTypeEnum.REFUND_PRODUCT)
                .description("Refund product: " + product.getName())
                .build();

        walletRepo.save(wallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(tx);

        return toResponse(order);
    }

    // 🟡 Hoàn hàng một phần
    @Override
    @Transactional
    public OrderResponse refundPartial(Long orderId, Integer quantityToRefund, Long staffUserId) {
        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatusEnum.REFUNDED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order already refunded");

        if (quantityToRefund <= 0 || quantityToRefund > order.getQuantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid refund quantity");

        Product product = order.getProduct();
        Optional<Club> existClub = clubRepo.findByClubId(product.getProductId());
        Wallet wallet = walletRepo.findByUser_UserIdAndClub_ClubId(staffUserId, existClub.get().getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        long refundPoints = product.getPointCost() * quantityToRefund; // ✅ Long-safe

        wallet.setBalancePoints(wallet.getBalancePoints() + refundPoints);
        product.setStockQuantity(product.getStockQuantity() + quantityToRefund);

        order.setQuantity(order.getQuantity() - quantityToRefund);
        order.setStatus(order.getQuantity() == 0
                ? OrderStatusEnum.REFUNDED
                : OrderStatusEnum.PARTIALLY_REFUNDED);
        order.setTotalPoints(product.getPointCost() * order.getQuantity());
        order.setCompletedAt(LocalDateTime.now());

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(refundPoints)
                .type(WalletTransactionTypeEnum.REFUND_PRODUCT)
                .description("Partial refund: " + product.getName() + " x" + quantityToRefund)
                .build();

        walletRepo.save(wallet);
        productRepo.save(product);
        walletTxRepo.save(tx);
        orderRepo.save(order);

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse complete(Long orderId, Long staffUserId) {
        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order not in PENDING");

        order.setStatus(OrderStatusEnum.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepo.save(order);
        return toResponse(order);
    }

    private boolean isEventStillActive(Event event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = event.getDate().atStartOfDay();
        LocalDateTime end = event.getDate().atTime(LocalTime.MAX);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByMember(Long userId) {
        return orderRepo.findByMembership_User_UserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByClub(Long clubId) {
        return orderRepo.findByClub_ClubId(clubId)
                .stream()
                .filter(order -> order.getProduct().getType() == ProductTypeEnum.CLUB_ITEM)
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByEvent(Long eventId) {
        return orderRepo.findByProduct_Event_EventId(eventId)
                .stream()
                .filter(order -> order.getProduct().getType() == ProductTypeEnum.EVENT_ITEM)
                .map(this::toResponse).toList();
    }
}
