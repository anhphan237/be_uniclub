package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubAttendanceRecord;
import com.example.uniclub.enums.AttendanceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClubAttendanceRecordRepository extends JpaRepository<ClubAttendanceRecord, Long> {
    List<ClubAttendanceRecord> findBySession_Id(Long sessionId);
    Optional<ClubAttendanceRecord> findBySession_IdAndMembership_MembershipId(Long sessionId, Long membershipId);
    List<ClubAttendanceRecord> findByMembership_MembershipId(Long membershipId);

    int countByMembership_User_UserIdAndSession_CreatedAtBetween(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    int countByMembership_User_UserIdAndStatusAndSession_CreatedAtBetween(
            Long userId,
            AttendanceStatusEnum status,
            LocalDateTime start,
            LocalDateTime end
    );

    List<ClubAttendanceRecord> findByMembership_User_UserIdAndSession_CreatedAtBetween(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );
    int countByMembership_User_UserIdAndStatusInAndSession_CreatedAtBetween(
            Long userId,
            List<AttendanceStatusEnum> statuses,
            LocalDateTime start,
            LocalDateTime end
    );
    // THÊM vào ClubAttendanceRecordRepository

    int countByMembership_MembershipIdAndSession_DateBetween(
            Long membershipId,
            java.time.LocalDate start,
            java.time.LocalDate end
    );

    int countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
            Long membershipId,
            java.util.List<AttendanceStatusEnum> statuses,
            java.time.LocalDate start,
            java.time.LocalDate end
    );
    // Đếm tổng số session mà CLB tổ chức trong tháng
    int countBySession_Club_ClubIdAndSession_DateBetween(
            Long clubId,
            LocalDate start,
            LocalDate end
    );

    // Đếm số attendance PRESENT hoặc LATE của CLB
    int countBySession_Club_ClubIdAndStatusInAndSession_DateBetween(
            Long clubId,
            List<AttendanceStatusEnum> statuses,
            LocalDate start,
            LocalDate end
    );


}
