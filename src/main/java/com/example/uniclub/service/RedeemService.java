package com.example.uniclub.service;

import com.example.uniclub.dto.request.RedeemOrderRequest;
import com.example.uniclub.dto.response.OrderResponse;
import com.example.uniclub.dto.response.RedeemScanResponse;

import java.util.List;

public interface RedeemService {

    // 🟢 Thành viên đổi hàng trong CLB
    OrderResponse createClubOrder(Long clubId, RedeemOrderRequest req, Long userId);

    // 🟢 Staff đổi hàng trong booth event
    OrderResponse eventRedeem(Long eventId, RedeemOrderRequest req, Long staffUserId);

    // 🟢 Staff xác nhận hoàn tất đơn hàng
    OrderResponse complete(Long orderId, Long staffUserId);

    // 🟡 Hoàn hàng toàn phần
    OrderResponse refund(Long orderId, Long staffUserId, String reason);

    // 🟡 Hoàn hàng một phần
    OrderResponse refundPartial(Long orderId, Integer quantityToRefund, Long staffUserId, String reason);


    OrderResponse getOrderByCode(String orderCode);

    OrderResponse getOrderById(Long orderId);

    // 🔹 Lấy danh sách đơn hàng của member / club / event
    List<OrderResponse> getOrdersByMember(Long userId);
    List<OrderResponse> getOrdersByClub(Long clubId);
    List<OrderResponse> getOrdersByEvent(Long eventId);
    // 🆕 Member tạo QR để đổi quà tại booth CLB
    String generateMemberQr(Long userId, Long clubId);
    List<OrderResponse> getEventOrdersByClub(Long clubId);

    // 🆕 Staff quét QR để xác thực member thuộc đúng CLB
    RedeemScanResponse scanMemberQr(String qrToken, Long staffUserId);

}
