package com.example.uniclub.repository;

import com.example.uniclub.entity.QRToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QRTokenRepository extends JpaRepository<QRToken, Long> {
    Optional<QRToken> findByTokenValue(String tokenValue);
}
