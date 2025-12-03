package com.example.uniclub.repository;

import com.example.uniclub.entity.ProductOrder;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.OrderStatusEnum;
import com.example.uniclub.enums.ProductTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<ProductOrder> findByMembership_MembershipIdAndStatus(
            Long membershipId,
            OrderStatusEnum status
    );
    List<ProductOrder> findByClub_ClubIdAndProduct_Type(Long clubId, ProductTypeEnum type);

    List<ProductOrder> findByMembership_MembershipId(Long membershipId);

    Page<ProductOrder> findByHandledBy_UserIdAndProduct_Event_EventIdOrderByCompletedAtDesc(
            Long staffUserId,
            Long eventId,
            Pageable pageable
    );
    Page<ProductOrder> findByHandledBy_UserIdOrderByCompletedAtDesc(
            Long staffUserId,
            Pageable pageable
    );
    @Query("""
    SELECT COALESCE(SUM(po.quantity), 0)
    FROM ProductOrder po
    JOIN po.product p
    LEFT JOIN p.event e
    LEFT JOIN e.coHostRelations r
    WHERE p.type = com.example.uniclub.enums.ProductTypeEnum.EVENT_ITEM
      AND (po.club.clubId = :clubId
           OR e.hostClub.clubId = :clubId
           OR r.club.clubId = :clubId)
""")
    Long sumEventProductsByClub(@Param("clubId") Long clubId);
    @Query("""
    SELECT DISTINCT o FROM ProductOrder o
    JOIN OrderActionLog l ON l.order = o
    WHERE l.actor.userId = :staffId
    ORDER BY o.completedAt DESC
""")
    Page<ProductOrder> findOrdersHandledByStaff(@Param("staffId") Long staffId, Pageable pageable);
    @Query("""
    SELECT DISTINCT o
    FROM ProductOrder o
    JOIN OrderActionLog l ON l.order = o
    WHERE l.actor.userId = :staffId
      AND o.product.event.eventId = :eventId
    ORDER BY o.completedAt DESC
""")
    Page<ProductOrder> findEventOrdersHandledByStaff(
            @Param("staffId") Long staffId,
            @Param("eventId") Long eventId,
            Pageable pageable
    );

}
