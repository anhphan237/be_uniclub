package com.example.uniclub.repository;

import com.example.uniclub.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface EventRepository extends JpaRepository<Event, Long> {
    // ✅ Lấy tất cả sự kiện theo club_id
    List<Event> findByClub_ClubId(Long clubId);
    Optional<Event> findByCheckInCode(String checkInCode);
}
