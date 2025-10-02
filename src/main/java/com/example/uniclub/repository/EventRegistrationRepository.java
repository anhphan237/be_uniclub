package com.example.uniclub.repository;

import com.example.uniclub.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventAndUser(Event e, User u);
    Optional<EventRegistration> findByEventAndUser(Event e, User u);
}
