package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubAttendanceRecord;
import com.example.uniclub.enums.AttendanceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
