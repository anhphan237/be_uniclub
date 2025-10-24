package com.example.uniclub.repository;

import com.example.uniclub.entity.EventStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventStaffRepository extends JpaRepository<EventStaff, Long> {
    List<EventStaff> findByEvent_EventId(Long eventId);
    boolean existsByEvent_EventIdAndMembership_MembershipId(Long eventId, Long membershipId);
    List<EventStaff> findByEvent_EventIdOrderByIdAsc(Long eventId);

}
