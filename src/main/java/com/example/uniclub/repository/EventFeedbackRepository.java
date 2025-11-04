package com.example.uniclub.repository;

import com.example.uniclub.entity.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {
    List<EventFeedback> findByEvent_EventId(Long eventId);
    List<EventFeedback> findByMembership_MembershipId(Long membershipId);
    boolean existsByEvent_EventIdAndMembership_MembershipId(Long eventId, Long membershipId);

}
