package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminTransactionResponse;
import com.example.uniclub.dto.response.AdminWalletResponse;
import com.example.uniclub.service.AdminWalletService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    @Operation(summary = "💳 Lấy danh sách ví (phân trang)")
    @GetMapping
    public ResponseEntity<Page<AdminWalletResponse>> getAllWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("walletId").descending());
        return ResponseEntity.ok(adminWalletService.getAllWallets(pageable));
    }

    @Operation(summary = "📜 Lấy danh sách giao dịch (phân trang)")
    @GetMapping("/transactions")
    public ResponseEntity<Page<AdminTransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionId").descending());
        return ResponseEntity.ok(adminWalletService.getAllTransactions(pageable));
    }

    @Operation(summary = "🔍 Xem chi tiết giao dịch")
    @GetMapping("/transactions/{id}")
    public ResponseEntity<AdminTransactionResponse> getTransactionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminWalletService.getTransactionDetail(id));
    }

    @Operation(summary = "⚙️ Điều chỉnh số dư ví thủ công (ADMIN_ADJUST)")
    @PostMapping("/{walletId}/adjust")
    public ResponseEntity<Void> adjustWalletBalance(
            @PathVariable Long walletId,
            @RequestParam long amount,
            @RequestParam(required = false, defaultValue = "Manual adjustment by admin") String note) {
        adminWalletService.adjustWalletBalance(walletId, amount, note);
        return ResponseEntity.ok().build();
    }
}
