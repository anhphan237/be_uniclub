package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.uniclub.repository.TagRepository;
import com.example.uniclub.repository.ProductTagRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedeemServiceImpl implements RedeemService {
    private final EventLogService eventLogService;
    private final TagRepository tagRepo;
    private final ProductTagRepository productTagRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProductRepository productRepo;
    private final ProductOrderRepository orderRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final MembershipRepository membershipRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final QrService qrService;
    private final EmailService emailService;
    private final NotificationRepository notificationRepo;
    private final WalletNotificationService walletNotificationService;


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
                o.getProduct().getType().name(),
                o.getClub().getClubId(),
                o.getProduct().getEvent() != null ? o.getProduct().getEvent().getEventId() : null,
                o.getClub().getName(),
                o.getMembership().getUser().getFullName(),
                o.getReasonRefund()
        );
    }

    // 🟢 Thành viên đổi hàng trong CLB
    @Override
    @Transactional
    public OrderResponse createClubOrder(Long clubId, RedeemOrderRequest req, Long userId) {

        // 🔹 Lấy CLB và kiểm tra hợp lệ
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // ⚠️ Lock product row để tránh 2 member redeem cùng lúc
        Product product = productRepo.findByIdForUpdate(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.getClub().getClubId().equals(clubId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product not belongs to this club");

        if (product.getStatus() != ProductStatusEnum.ACTIVE || !Boolean.TRUE.equals(product.getIsActive()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not ACTIVE");

        if (product.getType() != ProductTypeEnum.CLUB_ITEM)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not CLUB_ITEM");

        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubIdAndState(userId, clubId, MembershipStateEnum.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not active member of this club"));

        if (product.getStockQuantity() < req.quantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Out of stock");

        long totalPoints = product.getPointCost() * req.quantity();

        // 🧾 Ví user & CLB
        Wallet userWallet = walletRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        if (userWallet.getBalancePoints() < totalPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        // 🔁 Update ví
        userWallet.setBalancePoints(userWallet.getBalancePoints() - totalPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + totalPoints);

        // 🔁 Update sản phẩm
        product.setStockQuantity(product.getStockQuantity() - req.quantity());
        product.increaseRedeemCount(req.quantity());

        // 🔁 Tạo order
        ProductOrder order = ProductOrder.builder()
                .product(product)
                .membership(membership)
                .club(club)
                .quantity(req.quantity())
                .totalPoints(totalPoints)
                .status(OrderStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // 🔁 Giao dịch ví
        WalletTransaction txUser = WalletTransaction.builder()
                .wallet(userWallet)
                .amount(-totalPoints)
                .type(WalletTransactionTypeEnum.REDEEM_PRODUCT)
                .description("User redeemed product '" + product.getName() + "' from club " + club.getName())
                .senderName(membership.getUser().getFullName())
                .receiverName(club.getName())
                .build();

        WalletTransaction txClub = WalletTransaction.builder()
                .wallet(clubWallet)
                .amount(totalPoints)
                .type(WalletTransactionTypeEnum.CLUB_RECEIVE_REDEEM)
                .description("Received points from user '" + membership.getUser().getFullName() + "' redeeming " + product.getName())
                .senderName(membership.getUser().getFullName())
                .receiverName(club.getName())
                .build();

        // 💾 Save tất cả
        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        walletNotificationService.sendWalletTransactionNotification(txUser);
        walletNotificationService.sendWalletTransactionNotification(txClub);

        // 🧾 QR + Code
        String orderCode = "UC-" + Long.toHexString(order.getOrderId()).toUpperCase();
        String qrUrl = qrService.generateQrAndUpload(orderCode);

        order.setOrderCode(orderCode);
        order.setQrCodeBase64(qrUrl);
        orderRepo.save(order);

        // ⛳ **EMAIL SERVICE CHUẨN**
        emailService.sendClubRedeemEmail(
                membership.getUser().getEmail(),
                membership.getUser().getFullName(),
                product.getName(),
                req.quantity(),
                totalPoints,
                orderCode,
                qrUrl
        );


        // 📢 Realtime notification
        try {
            Notification notification = Notification.builder()
                    .user(membership.getUser())
                    .message("🎁 You redeemed '" + product.getName() + "' and spent " + totalPoints + " points.")
                    .type(NotificationTypeEnum.REDEEM)
                    .status(NotificationStatusEnum.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepo.save(notification);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(membership.getUser().getUserId()),
                    "/queue/notifications",
                    notification
            );

            log.info("📩 Sent realtime redeem notification to {}", membership.getUser().getFullName());
        } catch (Exception e) {
            log.error("❌ Failed to send realtime notification: {}", e.getMessage());
        }

        // 🧾 Log hoạt động
        User user = membership.getUser();
        Event event = product.getEvent();

        eventLogService.logAction(
                user.getUserId(),
                user.getFullName(),
                event != null ? event.getEventId() : null,
                event != null ? event.getName() : null,
                UserActionEnum.REDEEM_PRODUCT,
                "User redeemed product '" + product.getName() +
                        "' x" + req.quantity() + " from club " + club.getName()
        );

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse eventRedeem(Long eventId, RedeemOrderRequest req, Long staffUserId) {

        // 🔹 Kiểm tra sự kiện
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!isEventStillActive(event))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event has not started or has already ended");

        // 🔹 Check product
        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getType() != ProductTypeEnum.EVENT_ITEM)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not EVENT_ITEM");

        if (product.getEvent() == null || !product.getEvent().getEventId().equals(eventId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product does not belong to this event");

        if (product.getStatus() != ProductStatusEnum.ACTIVE || !Boolean.TRUE.equals(product.getIsActive()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not ACTIVE");

        if (product.getStockQuantity() < req.quantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Out of stock");

        // 🔹 Membership check
        Membership membership = membershipRepo.findById(req.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Long clubId = product.getClub().getClubId();

        if (!membership.getClub().getClubId().equals(clubId))
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership not belongs to this club");

        if (membership.getState() != MembershipStateEnum.ACTIVE)
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership is not ACTIVE");

        // 🔹 Ví user và CLB
        Wallet userWallet = walletRepo.findByUser_UserId(membership.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        long totalPoints = product.getPointCost() * req.quantity();

        if (userWallet.getBalancePoints() < totalPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        // 🔁 Update ví & stock
        userWallet.setBalancePoints(userWallet.getBalancePoints() - totalPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + totalPoints);
        product.setStockQuantity(product.getStockQuantity() - req.quantity());
        product.increaseRedeemCount(req.quantity());
        productRepo.save(product);

        // 🔥 Auto-tag HOT (>=50 redemption)
        try {
            if (product.getRedeemCount() >= 50) {
                tagRepo.findByNameIgnoreCase("hot").ifPresent(tagHot -> {
                    boolean exists = product.getProductTags().stream()
                            .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase("hot"));
                    if (!exists) {
                        productTagRepo.save(ProductTag.builder()
                                .product(product)
                                .tag(tagHot)
                                .build());
                    }
                });
            }
        } catch (Exception ignored) {}

        // 🔹 Tạo order
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

        // 🔹 Giao dịch ví
        WalletTransaction txUser = WalletTransaction.builder()
                .wallet(userWallet)
                .amount(-totalPoints)
                .type(WalletTransactionTypeEnum.EVENT_REDEEM_PRODUCT)
                .description("Event redeem: " + product.getName())
                .senderName(membership.getUser().getFullName())
                .receiverName(clubWallet.getClub().getName())
                .build();

        WalletTransaction txClub = WalletTransaction.builder()
                .wallet(clubWallet)
                .amount(totalPoints)
                .type(WalletTransactionTypeEnum.CLUB_RECEIVE_REDEEM)
                .description("Club received event redeem: " + product.getName())
                .senderName(membership.getUser().getFullName())
                .receiverName(clubWallet.getClub().getName())
                .build();

        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        walletNotificationService.sendWalletTransactionNotification(txUser);
        walletNotificationService.sendWalletTransactionNotification(txClub);

        // 🧾 Generate QR + order code
        String orderCode = "EV-" + Long.toHexString(order.getOrderId()).toUpperCase();
        String qrUrl = qrService.generateQrAndUpload(orderCode);

        order.setOrderCode(orderCode);
        order.setQrCodeBase64(qrUrl);
        orderRepo.save(order);

        // ⛳ EMAIL SERVICE CHUẨN
        emailService.sendEventRedeemEmail(
                membership.getUser().getEmail(),
                membership.getUser().getFullName(),
                event.getName(),
                product.getName(),
                req.quantity(),
                totalPoints,
                orderCode,
                qrUrl
        );

        // 🔔 Notification realtime
        try {
            Notification notification = Notification.builder()
                    .user(membership.getUser())
                    .message("🎉 You redeemed '" + product.getName() + "' at event '" + event.getName() +
                            "' and spent " + totalPoints + " points.")
                    .type(NotificationTypeEnum.REDEEM)
                    .status(NotificationStatusEnum.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepo.save(notification);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(membership.getUser().getUserId()),
                    "/queue/notifications",
                    notification
            );
        } catch (Exception ignored) {}

        // 🧾 Log hoạt động
        User user = membership.getUser();
        eventLogService.logAction(
                user.getUserId(),
                user.getFullName(),
                event.getEventId(),
                event.getName(),
                UserActionEnum.REDEEM_PRODUCT,
                "User redeemed event product '" + product.getName() + "' x" + req.quantity()
        );

        return toResponse(order);
    }

    // 🟡 Hoàn hàng toàn phần
    @Override
    @Transactional
    public OrderResponse refund(Long orderId, Long staffUserId, String reason) {

        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatusEnum.REFUNDED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order already refunded");

        if (order.getStatus() != OrderStatusEnum.COMPLETED &&
                order.getStatus() != OrderStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order not refundable");

        Product product = order.getProduct();

        if (product.getType() == ProductTypeEnum.EVENT_ITEM &&
                !isEventStillActive(product.getEvent())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot refund after event ended");
        }

        Long memberUserId = order.getMembership().getUser().getUserId();
        Long clubId = product.getClub().getClubId();

        Wallet userWallet = walletRepo.findByUser_UserId(memberUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        long refundPoints = order.getTotalPoints();

        if (clubWallet.getBalancePoints() < refundPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club wallet insufficient points");

        // 🔁 Refund points
        userWallet.setBalancePoints(userWallet.getBalancePoints() + refundPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() - refundPoints);

        // 🔁 Return stock
        product.setStockQuantity(product.getStockQuantity() + order.getQuantity());
        product.decreaseRedeemCount(order.getQuantity());

        // 🔁 Update order
        order.setStatus(OrderStatusEnum.REFUNDED);
        order.setCompletedAt(LocalDateTime.now());
        order.setReasonRefund(reason != null ? reason : "Refund processed by staff ID " + staffUserId);

        // 🔁 Log wallet transactions
        WalletTransaction txUser = WalletTransaction.builder()
                .wallet(userWallet)
                .amount(refundPoints)
                .type(WalletTransactionTypeEnum.REFUND_PRODUCT)
                .description("Refund product: " + product.getName())
                .senderName(product.getClub().getName())
                .receiverName(order.getMembership().getUser().getFullName())
                .build();

        WalletTransaction txClub = WalletTransaction.builder()
                .wallet(clubWallet)
                .amount(-refundPoints)
                .type(WalletTransactionTypeEnum.CLUB_REFUND)
                .description("Club refunded points for: " + product.getName())
                .senderName(product.getClub().getName())
                .receiverName(order.getMembership().getUser().getFullName())
                .build();

        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        // ⛳ EMAIL SERVICE CHUẨN
        emailService.sendRefundEmail(
                order.getMembership().getUser().getEmail(),
                order.getMembership().getUser().getFullName(),
                product.getName(),
                order.getQuantity(),
                refundPoints,
                order.getReasonRefund(),
                order.getOrderCode()
        );


        return toResponse(order);
    }

    // 🟡 Hoàn hàng một phần
    @Override
    @Transactional
    public OrderResponse refundPartial(Long orderId, Integer quantityToRefund, Long staffUserId, String reason) {

        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatusEnum.REFUNDED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order already refunded");

        if (order.getStatus() != OrderStatusEnum.COMPLETED &&
                order.getStatus() != OrderStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order status not refundable");

        if (quantityToRefund == null || quantityToRefund <= 0 || quantityToRefund > order.getQuantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid refund quantity");

        Product product = order.getProduct();

        if (product.getType() == ProductTypeEnum.EVENT_ITEM &&
                !isEventStillActive(product.getEvent())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot refund after event ended");
        }

        Long memberUserId = order.getMembership().getUser().getUserId();
        Long clubId = product.getClub().getClubId();

        Wallet userWallet = walletRepo.findByUser_UserId(memberUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        long refundPoints = product.getPointCost() * quantityToRefund;

        // ✅ Check club có đủ điểm refund
        if (clubWallet.getBalancePoints() < refundPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club wallet has insufficient points to refund");

        // 🔁 Cập nhật ví & kho
        userWallet.setBalancePoints(userWallet.getBalancePoints() + refundPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() - refundPoints);
        product.setStockQuantity(product.getStockQuantity() + quantityToRefund);
        product.decreaseRedeemCount(quantityToRefund);

        // 🔁 Update order
        order.setQuantity(order.getQuantity() - quantityToRefund);
        order.setTotalPoints(order.getQuantity() * product.getPointCost());
        order.setStatus(order.getQuantity() == 0
                ? OrderStatusEnum.REFUNDED
                : OrderStatusEnum.PARTIALLY_REFUNDED);

        order.setReasonRefund(reason != null ? reason :
                "Partial refund (" + quantityToRefund + " items) processed by staff ID " + staffUserId);

        order.setCompletedAt(LocalDateTime.now());

        // 🔁 Transactions
        WalletTransaction txUser = WalletTransaction.builder()
                .wallet(userWallet)
                .amount(refundPoints)
                .type(WalletTransactionTypeEnum.REFUND_PRODUCT)
                .description("Partial refund: " + product.getName() + " x" + quantityToRefund)
                .senderName(product.getClub().getName())
                .receiverName(order.getMembership().getUser().getFullName())
                .build();

        WalletTransaction txClub = WalletTransaction.builder()
                .wallet(clubWallet)
                .amount(-refundPoints)
                .type(WalletTransactionTypeEnum.CLUB_REFUND)
                .description("Club partial refund for: " + product.getName())
                .senderName(product.getClub().getName())
                .receiverName(order.getMembership().getUser().getFullName())
                .build();

        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        // ⛳ EMAIL SERVICE CHUẨN – THAY THẾ EMAIL THÔ
        emailService.sendPartialRefundEmail(
                order.getMembership().getUser().getEmail(),
                order.getMembership().getUser().getFullName(),
                product.getName(),
                quantityToRefund,
                refundPoints,
                order.getReasonRefund(),
                order.getOrderCode()
        );


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
