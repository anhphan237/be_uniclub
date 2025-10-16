package com.example.uniclub.repository;

import com.example.uniclub.entity.EventRegistration;
import com.example.uniclub.enums.RegistrationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEvent_EventIdAndUser_UserId(Long eventId, Long userId);

    Optional<EventRegistration> findByEvent_EventIdAndUser_UserId(Long eventId, Long userId);

    List<EventRegistration> findByEvent_EventIdAndStatus(Long eventId, RegistrationStatusEnum status);

    List<EventRegistration> findByEvent_EventId(Long eventId);
}
