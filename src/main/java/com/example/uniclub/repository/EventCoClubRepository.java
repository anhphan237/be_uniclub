package com.example.uniclub.repository;

import com.example.uniclub.entity.EventCoClub;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventCoClubRepository extends JpaRepository<EventCoClub, Long> {

    Optional<EventCoClub> findByEvent_EventIdAndClub_ClubId(Long eventId, Long clubId);

    List<EventCoClub> findAllByEvent_EventId(Long eventId);
}
