package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.enums.WalletStatusEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.repository.WalletTransactionRepository;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;

    // ================================================================
    // 🔍 LẤY VÍ THEO CLUB / USER / ID
    // ================================================================
    @Override
    public Wallet getWalletByClubId(Long clubId) {
        return walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Wallet not found for clubId: " + clubId));
    }

    @Override
    public Wallet getWalletById(Long walletId) {
        return walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Wallet not found: " + walletId));
    }

    // ================================================================
    // 🏗️ TẠO VÍ NẾU CHƯA CÓ
    // ================================================================
    @Override
    @Transactional
    public Wallet getOrCreateClubWallet(Club club) {
        return walletRepo.findByClub(club)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .club(club)
                        .ownerType(WalletOwnerTypeEnum.CLUB)
                        .balancePoints(0L)
                        .status(WalletStatusEnum.ACTIVE)
                        .build()));
    }


    @Override
    @Transactional
    public Wallet getOrCreateUserWallet(User user) {
        return walletRepo.findByUser(user)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .user(user)
                        .ownerType(WalletOwnerTypeEnum.USER)
                        .balancePoints(0L)
                        .status(WalletStatusEnum.ACTIVE)
                        .build()));
    }


    // ================================================================
    // 💰 TĂNG / GIẢM ĐIỂM
    // ================================================================
    @Override
    @Transactional
    public void increase(Wallet wallet, long points) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");
        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);
    }

    @Override
    @Transactional
    public void decrease(Wallet wallet, long points) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");
        if (wallet.getBalancePoints() < points)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");

        wallet.setBalancePoints(wallet.getBalancePoints() - points);
        walletRepo.save(wallet);
    }

    // ================================================================
    // 📜 GIAO DỊCH CÓ LOG
    // ================================================================
    @Override
    @Transactional
    public void addPoints(Wallet wallet, long points, String description) {
        increase(wallet, points);
        saveTransaction(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.ADD)
                .amount(points)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void reducePoints(Wallet wallet, long points, String description) {
        decrease(wallet, points);
        saveTransaction(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.REDUCE)
                .amount(points)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void transferPoints(Wallet from, Wallet to, long points, String description) {
        if (from.getBalancePoints() < points)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in source wallet");

        from.setBalancePoints(from.getBalancePoints() - points);
        to.setBalancePoints(to.getBalancePoints() + points);
        walletRepo.save(from);
        walletRepo.save(to);

        // OUT
        saveTransaction(WalletTransaction.builder()
                .wallet(from)
                .type(WalletTransactionTypeEnum.TRANSFER)
                .amount(-points)
                .description("[OUT] " + description)
                .createdAt(LocalDateTime.now())
                .build());

        // IN
        saveTransaction(WalletTransaction.builder()
                .wallet(to)
                .type(WalletTransactionTypeEnum.TRANSFER)
                .amount(points)
                .description("[IN] " + description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ================================================================
    // 🎓 NGHIỆP VỤ CHUYÊN BIỆT
    // ================================================================
    @Override
    @Transactional
    public void logUniToClubTopup(Wallet clubWallet, long points, String reason) {
        saveTransaction(WalletTransaction.builder()
                .wallet(clubWallet)
                .type(WalletTransactionTypeEnum.UNI_TO_CLUB)
                .amount(points)
                .description(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void logClubToMemberReward(Wallet memberWallet, long points, String reason) {
        saveTransaction(WalletTransaction.builder()
                .wallet(memberWallet)
                .type(WalletTransactionTypeEnum.CLUB_TO_MEMBER)
                .amount(points)
                .description(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ================================================================
    // 🧾 LỊCH SỬ GIAO DỊCH
    // ================================================================
    @Override
    public List<WalletTransaction> getTransactionsByWallet(Long walletId) {
        return txRepo.findByWallet_WalletIdOrderByCreatedAtDesc(walletId);
    }

    @Override
    public List<WalletTransactionResponse> getAllClubTopups() {
        return txRepo.findTopupFromUniStaff().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<WalletTransactionResponse> getAllMemberRewards() {
        return txRepo.findRewardToMembers().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getWalletTransactions(Long walletId) {
        return txRepo.findByWallet_WalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================================================================
    // 🧩 AUTO-FILL TÊN GỬI & NHẬN (chuẩn hóa)
    // ================================================================
    @Transactional
    public void saveTransaction(WalletTransaction tx) {
        Wallet w = walletRepo.findById(tx.getWallet().getWalletId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found when saving transaction"
                ));
        tx.setWallet(w);

        // ✅ Sender name: chỉ fill nếu NULL hoặc BLANK
        if (tx.getSenderName() == null || tx.getSenderName().isBlank()) {
            if (w.getUser() != null)
                tx.setSenderName(w.getUser().getFullName());
            else if (w.getClub() != null)
                tx.setSenderName(w.getClub().getName());
            else if (w.getEvent() != null)
                tx.setSenderName(w.getEvent().getName());
            else
                tx.setSenderName("System");
        }

        // ✅ Receiver name: chỉ fill nếu NULL hoặc BLANK
        if (tx.getReceiverName() == null || tx.getReceiverName().isBlank()) {
            if (w.getUser() != null)
                tx.setReceiverName(w.getUser().getFullName());
            else if (w.getClub() != null)
                tx.setReceiverName(w.getClub().getName());
            else if (w.getEvent() != null)
                tx.setReceiverName(w.getEvent().getName());
            else
                tx.setReceiverName("System");
        }

        if (tx.getCreatedAt() == null)
            tx.setCreatedAt(LocalDateTime.now());

        txRepo.save(tx);
    }



    // ================================================================
    // 🧾 MAP TRANSACTION → RESPONSE (có hiển thị + / −)
    // ================================================================
    private WalletTransactionResponse mapToResponse(WalletTransaction tx) {

        long amt = tx.getAmount();
        String signed = (amt > 0 ? "+" : "-") + Math.abs(amt);

        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .senderName(tx.getSenderName())
                .receiverName(tx.getReceiverName())
                .signedAmount(signed)
                .build();
    }


    // ================================================================
    // 🏫 NẠP ĐIỂM TỪ UNIVERSITY
    // ================================================================
    @Override
    @Transactional
    public WalletTransaction topupPointsFromUniversity(Wallet clubWallet, long points, String reason) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid points amount");

        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + points);
        walletRepo.save(clubWallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(clubWallet)
                .amount(points)
                .type(WalletTransactionTypeEnum.UNI_TO_CLUB)
                .description(reason)
                .senderName("University Staff")
                .receiverName(clubWallet.getClub().getName())
                .createdAt(LocalDateTime.now())
                .build();

        txRepo.save(tx);
        return tx;
    }

    @Override
    @Transactional
    public void topupPointsFromUniversityWithOperator(Long walletId, long points, String reason, String senderName) {
        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));

        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .amount(points)
                .type(WalletTransactionTypeEnum.UNI_TO_CLUB)
                .description(reason)
                .senderName(senderName)
                .receiverName(wallet.getClub().getName())
                .createdAt(LocalDateTime.now())
                .build();

        txRepo.save(tx);
    }

    // ================================================================
    // 💸 CHUYỂN ĐIỂM VỚI TYPE TUỲ CHỈNH
    // ================================================================
    @Transactional
    @Override
    public void transferPointsWithType(
            Wallet sender,
            Wallet receiver,
            long amount,
            String reason,
            WalletTransactionTypeEnum type
    ) {
        if (amount <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        if (sender.getBalancePoints() < amount)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance");

        // ===============================
        // 1️⃣ Update balances
        // ===============================
        sender.setBalancePoints(sender.getBalancePoints() - amount);
        receiver.setBalancePoints(receiver.getBalancePoints() + amount);
        walletRepo.save(sender);
        walletRepo.save(receiver);

        // ===============================
        // 2️⃣ Resolve display names (FIX EVENT)
        // ===============================
        String senderDisplay =
                sender.getUser() != null ? sender.getUser().getFullName()
                        : sender.getClub() != null ? sender.getClub().getName()
                        : sender.getEvent() != null ? sender.getEvent().getName()
                        : "System";

        String receiverDisplay =
                receiver.getUser() != null ? receiver.getUser().getFullName()
                        : receiver.getClub() != null ? receiver.getClub().getName()
                        : receiver.getEvent() != null ? receiver.getEvent().getName()
                        : "System";

        // ===============================
        // 3️⃣ OUT transaction
        // ===============================
        WalletTransaction outTx = WalletTransaction.builder()
                .wallet(sender)
                .type(type)
                .amount(-amount)
                .description("[OUT] " + reason)
                .senderName(senderDisplay)
                .receiverName(receiverDisplay)
                .createdAt(LocalDateTime.now())
                .build();
        saveTransaction(outTx);

        // ===============================
        // 4️⃣ IN transaction
        // ===============================
        WalletTransaction inTx = WalletTransaction.builder()
                .wallet(receiver)
                .type(type)
                .amount(amount)
                .description("[IN] " + reason)
                .senderName(senderDisplay)
                .receiverName(receiverDisplay)
                .createdAt(LocalDateTime.now())
                .build();
        saveTransaction(inTx);
    }



    // ================================================================
    // 🧾 GHI TRANSACTION TỪ SYSTEM
    // ================================================================
    @Transactional
    public void logTransactionFromSystem(
            Wallet wallet,
            long amount,
            WalletTransactionTypeEnum type,
            String reason
    ) {
        if (amount == 0) {
            return;
        }
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .description(reason)
                .senderName("[System]")
                .receiverName(wallet.getUser() != null ? wallet.getUser().getFullName()
                        : wallet.getClub() != null ? wallet.getClub().getName() : "System")
                .createdAt(LocalDateTime.now())
                .build();

        saveTransaction(tx);
    }
}
