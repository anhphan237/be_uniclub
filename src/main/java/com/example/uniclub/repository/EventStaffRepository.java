package com.example.uniclub.repository;

import com.example.uniclub.entity.EventStaff;
import com.example.uniclub.enums.EventStaffStateEnum;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventStaffRepository extends JpaRepository<EventStaff, Long> {

    // 🔹 Truy vấn cơ bản theo Event ID
    List<EventStaff> findByEvent_EventId(Long eventId);

    // 🔹 Kiểm tra xem membership có được assign vào event chưa
    boolean existsByEvent_EventIdAndMembership_MembershipId(Long eventId, Long membershipId);

    // 🔹 Lấy danh sách staff của event theo thứ tự ID tăng dần
    List<EventStaff> findByEvent_EventIdOrderByIdAsc(Long eventId);

    // 🔹 Đếm số staff của event theo trạng thái (ACTIVE, INACTIVE, ...)
    long countByEvent_EventIdAndState(Long eventId, EventStaffStateEnum state);

    // 🔹 Lấy danh sách EventStaff ACTIVE nhưng event đã kết thúc
    @Query("""
        SELECT es FROM EventStaff es
        JOIN es.event.days d
        WHERE es.state = 'ACTIVE'
          AND (
                d.date < CURRENT_DATE
             OR (d.date = CURRENT_DATE AND d.endTime <= CURRENT_TIME)
          )
    """)
    List<EventStaff> findActiveWhereEventEnded();


    // 🔹 Tìm EventStaff ACTIVE theo Event + Membership (để kiểm tra đang làm việc)
    @Query("""
        SELECT es FROM EventStaff es
        WHERE es.event.eventId = :eventId
          AND es.membership.membershipId = :membershipId
          AND es.state = 'ACTIVE'
        """)
    Optional<EventStaff> findActiveByEventAndMembership(
            @Param("eventId") Long eventId,
            @Param("membershipId") Long membershipId
    );

    long countByMembership_MembershipIdAndState(Long membershipId, EventStaffStateEnum state);

    List<EventStaff> findByEvent_EventIdAndState(Long eventId, EventStaffStateEnum state);
    @Query("""
    SELECT COUNT(es) > 0
    FROM EventStaff es
    WHERE es.membership.membershipId = :membershipId
      AND es.state IN :states
""")
    boolean isMemberStaff(
            @Param("membershipId") Long membershipId,
            @Param("states") List<EventStaffStateEnum> states
    );
    boolean existsByMembership_MembershipIdAndStateIn(Long membershipId, List<EventStaffStateEnum> states);


    Optional<EventStaff> findByEvent_EventIdAndMembership_MembershipId(
            Long eventId,
            Long membershipId
    );

    // 🔹 Lấy các lần làm staff theo membership + event.date
    @Query("""
    SELECT es FROM EventStaff es
    WHERE es.membership.membershipId = :membershipId
      AND es.event.startDate BETWEEN :start AND :end
      AND es.state = com.example.uniclub.enums.EventStaffStateEnum.ACTIVE
    """)
    List<EventStaff> findByMembershipAndEventDateBetween(
            @Param("membershipId") Long membershipId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
    @Query("""
    SELECT es FROM EventStaff es
    WHERE es.membership.user.userId = :userId
      AND es.state = 'ACTIVE'
""")
    List<EventStaff> findActiveStaffByUserId(@Param("userId") Long userId);
    @Query("""
    SELECT COUNT(es)
    FROM EventStaff es
    JOIN es.event e
    LEFT JOIN e.coHostRelations r
    WHERE es.state = com.example.uniclub.enums.EventStaffStateEnum.ACTIVE
      AND (e.hostClub.clubId = :clubId OR r.club.clubId = :clubId)
""")
    Long countStaffAssignmentsByClub(@Param("clubId") Long clubId);

}
