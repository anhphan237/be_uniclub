package com.example.uniclub.repository;

import com.example.uniclub.entity.ProductOrder;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.OrderStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    // 🔹 Lấy danh sách đơn hàng theo thành viên
    List<ProductOrder> findByMembershipOrderByCreatedAtDesc(Membership membership);

    // 🔹 Đếm số đơn hàng theo trạng thái
    long countByMembershipAndStatus(Membership membership, OrderStatusEnum status);

    // 🔹 Lấy tất cả đơn đổi quà của một user
    List<ProductOrder> findByMembership_User_UserId(Long userId);

    // 🔹 Lấy tất cả đơn đổi quà của một CLB
    List<ProductOrder> findByClub_ClubId(Long clubId);

    // 🔹 Lấy tất cả đơn đổi quà của một event
    List<ProductOrder> findByProduct_Event_EventId(Long eventId);

    // 🔍 Tra cứu đơn hàng bằng mã UC-xxxxxx
    Optional<ProductOrder> findByOrderCode(String orderCode);
    // 🧾 Lấy toàn bộ đơn hàng (phân trang + sort)
    Page<ProductOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
