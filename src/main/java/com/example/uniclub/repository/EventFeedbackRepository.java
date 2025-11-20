package com.example.uniclub.repository;

import com.example.uniclub.entity.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {
    List<EventFeedback> findByEvent_EventId(Long eventId);
    List<EventFeedback> findByMembership_MembershipId(Long membershipId);
    boolean existsByEvent_EventIdAndMembership_MembershipId(Long eventId, Long membershipId);

    @Query("""
    SELECT f FROM EventFeedback f
    JOIN f.event e
    LEFT JOIN e.coHostRelations ec
    WHERE e.hostClub.clubId = :clubId OR ec.club.clubId = :clubId
""")
    List<EventFeedback> findAllByClubOrCoHost(@Param("clubId") Long clubId);

    List<EventFeedback> findByMembership_User_UserId(Long userId);

    Optional<EventFeedback> findByEvent_EventIdAndMembership_MembershipId(Long eventId, Long membershipId);

}
