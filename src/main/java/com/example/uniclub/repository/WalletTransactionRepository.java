package com.example.uniclub.repository;

import com.example.uniclub.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId);
}
