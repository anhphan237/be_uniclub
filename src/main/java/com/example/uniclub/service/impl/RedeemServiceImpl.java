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
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        Product product = productRepo.findById(req.productId())
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

        // 🧾 Ví của sinh viên & ví CLB
        Wallet userWallet = walletRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        if (userWallet.getBalancePoints() < totalPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        // 🔁 1️⃣ Trừ ví user, cộng ví CLB
        userWallet.setBalancePoints(userWallet.getBalancePoints() - totalPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + totalPoints);

        // 🧾 2️⃣ Cập nhật sản phẩm
        product.setStockQuantity(product.getStockQuantity() - req.quantity());
        product.increaseRedeemCount(req.quantity());

        // 🧾 3️⃣ Ghi đơn hàng
        ProductOrder order = ProductOrder.builder()
                .product(product)
                .membership(membership)
                .club(club)
                .quantity(req.quantity())
                .totalPoints(totalPoints)
                .status(OrderStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // 🧾 4️⃣ Ghi 2 giao dịch
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

        // 💾 Lưu toàn bộ thay đổi
        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        // 🧾 Gán mã đơn & QR
        String orderCode = "UC-" + Long.toHexString(order.getOrderId()).toUpperCase();
        String qrBase64 = qrService.generateQrAsBase64(orderCode);
        order.setOrderCode(orderCode);
        order.setQrCodeBase64(qrBase64);
        orderRepo.save(order);

        // 📧 Email xác nhận
        String memberEmail = membership.getUser().getEmail();
        String content = """
<h3>You have successfully redeemed your product!</h3>
<p><b>Product:</b> %s</p>
<p><b>Quantity:</b> %d</p>
<p><b>Points deducted:</b> %d</p>
<p><b>Order code:</b> %s</p>
<div style='text-align:center;margin:20px 0'>
    <img src="data:image/png;base64,%s" alt="QR Code" style="width:150px"/>
</div>
""".formatted(product.getName(), req.quantity(), totalPoints, orderCode, qrBase64);

        emailService.sendEmail(memberEmail, "[UniClub] Redemption Confirmation #" + orderCode, content);
        return toResponse(order);

    }


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

        if (product.getStatus() != ProductStatusEnum.ACTIVE || !Boolean.TRUE.equals(product.getIsActive()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Product is not ACTIVE");

        if (product.getStockQuantity() < req.quantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Out of stock");

        Membership membership = membershipRepo.findById(req.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Long clubId = product.getClub().getClubId();

        // ✅ Kiểm tra membership có đúng CLB và ACTIVE
        if (!membership.getClub().getClubId().equals(clubId))
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership not belongs to this club");
        if (membership.getState() != MembershipStateEnum.ACTIVE)
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership is not ACTIVE");

        Wallet userWallet = walletRepo.findByUser_UserId(membership.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        long totalPoints = product.getPointCost() * req.quantity();

        if (userWallet.getBalancePoints() < totalPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        // 🔁 Trừ user, cộng CLB
        userWallet.setBalancePoints(userWallet.getBalancePoints() - totalPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + totalPoints);

        // Cập nhật sản phẩm
        product.setStockQuantity(product.getStockQuantity() - req.quantity());
        product.increaseRedeemCount(req.quantity());

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
                .description("Club received event redeem from " + membership.getUser().getFullName())
                .senderName(membership.getUser().getFullName())
                .receiverName(clubWallet.getClub().getName())
                .build();

        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        String orderCode = "EV-" + Long.toHexString(order.getOrderId()).toUpperCase();
        String qrBase64 = qrService.generateQrAsBase64(orderCode);
        order.setOrderCode(orderCode);
        order.setQrCodeBase64(qrBase64);
        orderRepo.save(order);

        String memberEmail = membership.getUser().getEmail();
        String content = """
<h3>Gift redemption at the event was successful!</h3>
<p><b>Product:</b> %s</p>
<p><b>Quantity:</b> %d</p>
<p><b>Points deducted:</b> %d</p>
<p><b>Order code:</b> %s</p>
""".formatted(product.getName(), req.quantity(), totalPoints, orderCode);

        emailService.sendEmail(memberEmail, "[UniClub] Gift Redemption at Event " + event.getName(), content);
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

        // ✅ Chỉ cho phép refund nếu đơn đã hoàn tất hoặc đang pending
        if (order.getStatus() != OrderStatusEnum.COMPLETED && order.getStatus() != OrderStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order status not refundable");

        Product product = order.getProduct();

        if (product.getType() == ProductTypeEnum.EVENT_ITEM && !isEventStillActive(product.getEvent())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot refund after event ended");
        }

        Long memberUserId = order.getMembership().getUser().getUserId();
        Long clubId = product.getClub().getClubId();

        Wallet userWallet = walletRepo.findByUser_UserId(memberUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        long refundPoints = order.getTotalPoints();

        // ✅ Kiểm tra CLB đủ điểm hoàn
        if (clubWallet.getBalancePoints() < refundPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club wallet has insufficient points to refund");

        // 🔁 Hoàn điểm & cập nhật kho
        userWallet.setBalancePoints(userWallet.getBalancePoints() + refundPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() - refundPoints);
        product.setStockQuantity(product.getStockQuantity() + order.getQuantity());
        product.decreaseRedeemCount(order.getQuantity());
        order.setStatus(OrderStatusEnum.REFUNDED);
        order.setReasonRefund(reason);
        order.setCompletedAt(LocalDateTime.now());
        order.setReasonRefund("Full refund processed by staff ID " + staffUserId);
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
                .description("Club refunded points for product: " + product.getName())
                .senderName(product.getClub().getName())
                .receiverName(order.getMembership().getUser().getFullName())
                .build();

        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);

        String memberEmail = order.getMembership().getUser().getEmail();
        String content = """
<h3>Points refund successful!</h3>
<p><b>Product:</b> %s</p>
<p><b>Quantity refunded:</b> %d</p>
<p><b>Points refunded:</b> %d</p>
<p><b>Reason for refund:</b> %s</p>
""".formatted(
                product.getName(),
                order.getQuantity(),
                refundPoints,
                order.getReasonRefund() != null ? order.getReasonRefund() : "No specific reason provided"
        );

        emailService.sendEmail(
                memberEmail,
                "[UniClub] Points Refund Successful for Order #" + order.getOrderCode(),
                content
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

        if (order.getStatus() != OrderStatusEnum.COMPLETED && order.getStatus() != OrderStatusEnum.PENDING)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order status not refundable");

        if (quantityToRefund == null || quantityToRefund <= 0 || quantityToRefund > order.getQuantity())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid refund quantity");

        Product product = order.getProduct();

        if (product.getType() == ProductTypeEnum.EVENT_ITEM && !isEventStillActive(product.getEvent())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot refund after event ended");
        }

        Long memberUserId = order.getMembership().getUser().getUserId();
        Long clubId = product.getClub().getClubId();

        Wallet userWallet = walletRepo.findByUser_UserId(memberUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        long refundPoints = product.getPointCost() * quantityToRefund;

        // ✅ Kiểm tra CLB đủ điểm
        if (clubWallet.getBalancePoints() < refundPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club wallet has insufficient points to refund");

        // 🔁 Cập nhật ví & đơn hàng
        userWallet.setBalancePoints(userWallet.getBalancePoints() + refundPoints);
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() - refundPoints);
        product.setStockQuantity(product.getStockQuantity() + quantityToRefund);
        product.decreaseRedeemCount(quantityToRefund);

        order.setQuantity(order.getQuantity() - quantityToRefund);
        order.setStatus(order.getQuantity() == 0
                ? OrderStatusEnum.REFUNDED
                : OrderStatusEnum.PARTIALLY_REFUNDED);
        order.setTotalPoints(product.getPointCost() * order.getQuantity());
        order.setReasonRefund(reason);
        order.setCompletedAt(LocalDateTime.now());

        order.setReasonRefund("Partial refund (" + quantityToRefund + " items) processed by staff ID " + staffUserId);

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
                .description("Club partial refund for product: " + product.getName() + " x" + quantityToRefund)
                .senderName(product.getClub().getName())
                .receiverName(order.getMembership().getUser().getFullName())
                .build();

        walletRepo.save(userWallet);
        walletRepo.save(clubWallet);
        productRepo.save(product);
        orderRepo.save(order);
        walletTxRepo.save(txUser);
        walletTxRepo.save(txClub);
        // 📧 Gửi email thông báo hoàn điểm một phần
        String memberEmail = order.getMembership().getUser().getEmail();
        String content = """
<h3>Partial points refund!</h3>
<p><b>Product:</b> %s</p>
<p><b>Quantity refunded:</b> %d</p>
<p><b>Points refunded:</b> %d</p>
<p><b>Reason for refund:</b> %s</p>
""".formatted(
                product.getName(),
                quantityToRefund,
                refundPoints,
                order.getReasonRefund() != null ? order.getReasonRefund() : "No specific reason provided"
        );

        emailService.sendEmail(
                memberEmail,
                "[UniClub] Partial Points Refund for Order #" + order.getOrderCode(),
                content
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
