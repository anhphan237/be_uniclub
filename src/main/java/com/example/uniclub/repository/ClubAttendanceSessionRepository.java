package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClubAttendanceSessionRepository extends JpaRepository<ClubAttendanceSession, Long> {
    Optional<ClubAttendanceSession> findByClub_ClubIdAndDate(Long clubId, LocalDate date);
    List<ClubAttendanceSession> findByDateAndIsLockedFalse(LocalDate date);
    List<ClubAttendanceSession> findByDateAndIsLockedTrue(LocalDate date);

}
