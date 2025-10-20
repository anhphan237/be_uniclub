package com.example.uniclub.repository;

import com.example.uniclub.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser_UserId(Long userId);
    Optional<Wallet> findByClub_ClubId(Long clubId);

}

