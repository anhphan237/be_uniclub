package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.dto.response.RedeemScanResponse;
import com.example.uniclub.dto.response.ReturnImageResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

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
    private final ReturnImageRepository returnImageRepo;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepo;


    private OrderResponse toResponse(ProductOrder o) {
        List<String> images = returnImageRepo.findByOrder_OrderId(o.getOrderId())
                .stream()
                .map(img -> img.getImageUrl())
                .toList();
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
                o.getReasonRefund(),
                images
        );
    }
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        ProductOrder order = orderRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        return toResponse(order);
    }
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        return toResponse(order);
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
        String qrContent = orderCode;
        String qrUrl = qrService.generateQrAndUpload(qrContent);


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
        // 📣 EMAIL cho LEADER + VICE
        List<Membership> managers = membershipRepo
                .findByClub_ClubIdAndClubRoleInAndState(
                        clubId,
                        List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER),
                        MembershipStateEnum.ACTIVE
                );

        for (Membership m : managers) {
            emailService.sendMemberRedeemNotifyLeaderEmail(
                    m.getUser().getEmail(),
                    m.getUser().getFullName(),
                    membership.getUser().getFullName(),
                    membership.getUser().getStudentCode(),
                    product.getName(),
                    req.quantity(),
                    totalPoints,
                    orderCode
            );
        }
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
        String qrContent = orderCode;
        String qrUrl = qrService.generateQrAndUpload(qrContent);



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

        // =============================================================
        // 🔥 CHECK: MUST HAVE refund images uploaded BEFORE REFUND
        // =============================================================
        List<ReturnImage> images = returnImageRepo.findByOrder_OrderId(orderId);
        if (images.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload refund images before refunding.");
        }

        // =============================================================
        // 🔁 Refund Points
        // =============================================================
        userWallet.setBalancePoints(userWallet.getBalancePoints() + refundPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() - refundPoints);

        product.setStockQuantity(product.getStockQuantity() + order.getQuantity());
        product.decreaseRedeemCount(order.getQuantity());

        order.setStatus(OrderStatusEnum.REFUNDED);
        order.setCompletedAt(LocalDateTime.now());
        order.setReasonRefund(reason);

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

        if (clubWallet.getBalancePoints() < refundPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club wallet has insufficient points");

        // =============================================================
        // 🔥 CHECK: MUST HAVE refund images uploaded BEFORE REFUND
        // =============================================================
        List<ReturnImage> images = returnImageRepo.findByOrder_OrderId(orderId);
        if (images.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload refund images before refunding.");
        }

        // =============================================================
        // 🔁 Update wallets & stock
        // =============================================================
        userWallet.setBalancePoints(userWallet.getBalancePoints() + refundPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() - refundPoints);

        product.setStockQuantity(product.getStockQuantity() + quantityToRefund);
        product.decreaseRedeemCount(quantityToRefund);

        order.setQuantity(order.getQuantity() - quantityToRefund);
        order.setTotalPoints(order.getQuantity() * product.getPointCost());

        order.setStatus(order.getQuantity() == 0
                ? OrderStatusEnum.REFUNDED
                : OrderStatusEnum.PARTIALLY_REFUNDED);

        order.setReasonRefund(reason);
        order.setCompletedAt(LocalDateTime.now());

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

        // Lấy staff xử lý đơn
        User staff = userRepo.findById(staffUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff not found"));

        // Ghi nhận staff xử lý
        order.setHandledBy(staff);

        // Cập nhật trạng thái
        order.setStatus(OrderStatusEnum.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        orderRepo.save(order);

        return toResponse(order);
    }



    private boolean isEventStillActive(Event event) {

        if (event.getDays() == null || event.getDays().isEmpty()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        EventDay earliest = event.getDays().stream().min(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .orElse(null);

        EventDay latest = event.getDays().stream().max(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getEndTime))
                .orElse(null);

        if (earliest == null || latest == null) return false;

        LocalDateTime eventStart = LocalDateTime.of(earliest.getDate(), earliest.getStartTime());
        LocalDateTime eventEnd   = LocalDateTime.of(latest.getDate(), latest.getEndTime());

        return !now.isBefore(eventStart) && !now.isAfter(eventEnd);
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
    @Override
    public String generateMemberQr(Long userId, Long clubId) {

        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubIdAndState(
                        userId, clubId, MembershipStateEnum.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not an active member of this club"));

        LocalDateTime expires = LocalDateTime.now().plusSeconds(60);

        String raw = membership.getMembershipId() + ";" +
                clubId + ";" +
                userId + ";" +
                expires;

        // 🔐 Thêm hash để chống giả mạo QR
        String hash = Integer.toHexString(raw.hashCode());

        String full = raw + ";" + hash;

        String token = Base64.getEncoder().encodeToString(full.getBytes());

        return qrService.generateQrAndUpload(token);
    }
    @Override
    public RedeemScanResponse scanMemberQr(String qrToken, Long staffUserId) {

        // =======================
        // 1. Decode QR
        // =======================
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(qrToken));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid QR format");
        }

        // format: membershipId;clubId;userId;expires;hash
        String[] parts = decoded.split(";");
        if (parts.length != 5)
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR data invalid");

        Long membershipId = Long.parseLong(parts[0]);
        Long clubId = Long.parseLong(parts[1]);
        Long userId = Long.parseLong(parts[2]);
        LocalDateTime expires = LocalDateTime.parse(parts[3]);
        String qrHash = parts[4];

        // =======================
        // 2. Check hash (anti-fake)
        // =======================
        String raw = membershipId + ";" + clubId + ";" + userId + ";" + expires;
        String expected = Integer.toHexString(raw.hashCode());

        if (!expected.equals(qrHash))
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR signature invalid");

        // =======================
        // 3. Check expiration
        // =======================
        if (expires.isBefore(LocalDateTime.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR has expired (over 60 seconds)");

        // =======================
        // 4. Staff must belong to same club
        // =======================
        membershipRepo.findByUser_UserIdAndClub_ClubIdAndState(
                staffUserId, clubId, MembershipStateEnum.ACTIVE
        ).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "You are not a staff of this club")
        );

        // =======================
        // 5. Load Member Info
        // =======================
        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (membership.getState() != MembershipStateEnum.ACTIVE)
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership is not ACTIVE");

        User user = membership.getUser();

        // =======================
        // 6. Load Wallet
        // =======================
        Wallet wallet = walletRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // =======================
        // 7. Load pending orders (club storage)
        // =======================
        List<ProductOrder> pendingOrders =
                orderRepo.findByMembership_MembershipIdAndStatus(
                        membershipId, OrderStatusEnum.PENDING
                );

        // =======================
        // 8. Build response
        // =======================
        return RedeemScanResponse.from(user, membership, wallet, pendingOrders);
    }
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getEventOrdersByClub(Long clubId) {

        return orderRepo.findByClub_ClubIdAndProduct_Type(
                        clubId,
                        ProductTypeEnum.EVENT_ITEM
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<String> uploadRefundImages(Long orderId, List<MultipartFile> files) {

        ProductOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload at least one image.");
        }

        if (files.size() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can upload up to 5 images only.");
        }

        List<String> urls = new ArrayList<>();

        int nextOrder = returnImageRepo.countByOrder_OrderId(orderId);

        for (MultipartFile file : files) {

            if (file.getContentType() == null ||
                    !file.getContentType().toLowerCase().startsWith("image/")) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid file type: " + file.getOriginalFilename());
            }

            try {
                Map<?, ?> result = cloudinaryService.uploadRefundImageRaw(file, orderId);

                String url = (String) result.get("secure_url");
                String publicId = (String) result.get("public_id");

                ReturnImage img = ReturnImage.builder()
                        .order(order)
                        .imageUrl(url)
                        .publicId(publicId)
                        .displayOrder(nextOrder++)
                        .build();

                returnImageRepo.save(img);
                urls.add(url);

            } catch (Exception e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to upload image: " + file.getOriginalFilename());
            }
        }

        return urls;
    }



    @Override
    @Transactional
    public void deleteRefundImage(Long orderId, Long imageId) {

        ReturnImage img = returnImageRepo.findById(imageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Refund image not found"));

        if (!img.getOrder().getOrderId().equals(orderId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Image does not belong to this order");
        }

        try {
            cloudinaryService.deleteRefundImage(img.getPublicId());
        } catch (Exception ignored) {}

        returnImageRepo.delete(img);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnImageResponse> listRefundImages(Long orderId) {

        List<ReturnImage> images = returnImageRepo.findByOrder_OrderIdOrderByDisplayOrderAsc(orderId);

        return images.stream()
                .map(img -> new ReturnImageResponse(
                        img.getId(),
                        img.getImageUrl(),
                        img.getPublicId(),
                        img.getDisplayOrder()
                ))
                .toList();
    }

    @Override
    public Page<OrderResponse> getStaffApprovedOrders(Long staffUserId, Long eventId, Pageable pageable) {
        Page<ProductOrder> orders = orderRepo
                .findByHandledBy_UserIdAndProduct_Event_EventIdOrderByCompletedAtDesc(
                        staffUserId, eventId, pageable
                );

        return orders.map(this::toResponse);
    }

}
