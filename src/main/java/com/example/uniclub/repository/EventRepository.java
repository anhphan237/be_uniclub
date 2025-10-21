package com.example.uniclub.repository;

import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByCheckInCode(String checkInCode);

    List<Event> findByClub_ClubId(Long clubId);

    List<Event> findByDateAfter(LocalDate date);

    @Query("SELECT r.event FROM EventRegistration r WHERE r.user.userId = :userId")
    List<Event> findEventsByUserId(@Param("userId") Long userId);
    long countByStatus(EventStatusEnum status);

}
