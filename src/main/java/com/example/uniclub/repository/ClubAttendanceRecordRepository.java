package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClubAttendanceRecordRepository extends JpaRepository<ClubAttendanceRecord, Long> {
    List<ClubAttendanceRecord> findBySession_Id(Long sessionId);
    Optional<ClubAttendanceRecord> findBySession_IdAndMembership_MembershipId(Long sessionId, Long membershipId);
    List<ClubAttendanceRecord> findByMembership_MembershipId(Long membershipId);

}
