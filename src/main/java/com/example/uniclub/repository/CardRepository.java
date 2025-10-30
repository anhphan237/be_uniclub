package com.example.uniclub.repository;

import com.example.uniclub.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByClub_ClubId(Long clubId);
    Optional<Card> findFirstByClub_ClubId(Long clubId);
}
