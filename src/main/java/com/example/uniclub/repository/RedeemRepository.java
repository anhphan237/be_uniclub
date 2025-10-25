package com.example.uniclub.repository;

import com.example.uniclub.entity.Redeem;
import com.example.uniclub.enums.RedeemStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedeemRepository extends JpaRepository<Redeem, Long> {
    Optional<Redeem> findByRedeemIdAndStatus(Long id, RedeemStatusEnum status);
}
