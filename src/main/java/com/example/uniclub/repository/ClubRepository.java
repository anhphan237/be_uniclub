package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    boolean existsByName(String name);
    Optional<Club> findByLeader_UserId(Long userId);
    // ✅ Tìm club theo userId của leader
    Optional<Club> findByClubId(Long id);
}
