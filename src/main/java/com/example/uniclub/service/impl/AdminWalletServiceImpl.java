package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminTransactionResponse;
import com.example.uniclub.dto.response.AdminWalletResponse;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.repository.WalletTransactionRepository;
import com.example.uniclub.service.AdminWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminWalletServiceImpl implements AdminWalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTxRepo;

    // ============================================================
    // 📦 Lấy danh sách tất cả ví (phân trang)
    // ============================================================
    @Override
    public Page<AdminWalletResponse> getAllWallets(Pageable pageable) {
        Page<Wallet> wallets = walletRepo.findAll(pageable);
        return wallets.map(this::toWalletResp);
    }

    // ============================================================
    // 📜 Lấy danh sách tất cả giao dịch (phân trang)
    // ============================================================
    @Override
    public Page<AdminTransactionResponse> getAllTransactions(Pageable pageable) {
        Page<WalletTransaction> txs = walletTxRepo.findAll(pageable);
        return txs.map(this::toTxResp);
    }

    // ============================================================
    // 🔍 Lấy chi tiết giao dịch
    // ============================================================
    @Override
    public AdminTransactionResponse getTransactionDetail(Long id) {
        WalletTransaction tx = walletTxRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        return toTxResp(tx);
    }

    // ============================================================
    // ⚙️ Admin điều chỉnh số dư ví (ADMIN_ADJUST)
    // ============================================================
    @Override
    public void adjustWalletBalance(Long walletId, long amount, String note) {
        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // 🪙 Cập nhật số dư
        wallet.setBalancePoints(wallet.getBalancePoints() + amount);
        walletRepo.save(wallet);

        // 🧾 Ghi log transaction
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.ADMIN_ADJUST)
                .amount(amount)
                .description(note != null ? note : "Manual adjustment by admin")
                .senderName("System Admin")
                .receiverName(wallet.getDisplayName())
                .createdAt(LocalDateTime.now())
                .build();

        walletTxRepo.save(tx);
    }

    // ============================================================
    // 🧩 Helper mapping methods
    // ============================================================
    private AdminWalletResponse toWalletResp(Wallet w) {
        return AdminWalletResponse.builder()
                .id(w.getWalletId())
                .ownerName(w.getDisplayName())
                .walletType(w.getOwnerType().name())
                .balance(w.getBalancePoints())
                .build();
    }

    private AdminTransactionResponse toTxResp(WalletTransaction tx) {
        return AdminTransactionResponse.builder()
                .id(tx.getId())
                .senderName(tx.getSenderName())
                .receiverName(tx.getReceiverName())
                .type(tx.getType())
                .amount(tx.getAmount())
                .createdAt(tx.getCreatedAt())
                .note(tx.getDescription()) // ✅ Map description → note
                .build();
    }
}
