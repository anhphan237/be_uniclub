package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ProductTxStatusEnum;
import com.example.uniclub.enums.RedeemStatusEnum;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.RedeemService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RedeemServiceImpl implements RedeemService {

    private final MembershipRepository membershipRepo;
    private final ProductRepository productRepo;
    private final WalletRepository walletRepo;
    private final RedeemRepository redeemRepo;
    private final ProductTransactionRepository productTxRepo;
    private final WalletService walletService;

    @Override
    @Transactional
    public Redeem redeemProduct(Long userId, Long productId, Integer quantity, Long eventId) {
        if (quantity == null || quantity <= 0) quantity = 1;

        // 🔍 Tìm membership đang hoạt động của user
        Membership membership = membershipRepo.findAll().stream()
                .filter(m -> m.getUser() != null
                        && m.getUser().getUserId().equals(userId)
                        && m.getState().name().equals("ACTIVE"))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active membership not found for user."));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int totalCost = product.getPricePoints() * quantity;

        // 🪙 Dùng ví Membership thay vì User Wallet
        Wallet wallet = walletService.getOrCreateMembershipWallet(membership);

        if (wallet.getOwnerType() != WalletOwnerTypeEnum.MEMBERSHIP)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid wallet owner type");

        if (wallet.getBalancePoints() < totalCost)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        // Trừ điểm và lưu ví
        wallet.setBalancePoints(wallet.getBalancePoints() - totalCost);
        walletRepo.save(wallet);

        // 💾 Tạo bản ghi redeem
        Redeem redeem = Redeem.builder()
                .member(membership)
                .event(eventId == null ? null : Event.builder().eventId(eventId).build())
                .product(product)
                .quantity(quantity)
                .totalCostPoints(totalCost)
                .status(RedeemStatusEnum.PENDING)
                .build();
        redeemRepo.save(redeem);

        // 📦 Ghi log giao dịch sản phẩm
        ProductTransaction tx = ProductTransaction.builder()
                .product(product)
                .redeem(redeem)
                .quantity(quantity)
                .status(ProductTxStatusEnum.RESERVED)
                .build();
        productTxRepo.save(tx);

        return redeem;
    }
}
