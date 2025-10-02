package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {
    boolean existsByName(String name);
}
