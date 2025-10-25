package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ProductTxStatusEnum;
import com.example.uniclub.enums.RedeemStatusEnum;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.RedeemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RedeemServiceImpl implements RedeemService {

    private final MembershipRepository membershipRepository;
    private final ProductRepository productRepository;
    private final WalletRepository walletRepository;
    private final RedeemRepository redeemRepository;
    private final ProductTransactionRepository productTxRepository;

    @Override
    @Transactional
    public Redeem redeemProduct(Long userId, Long productId, Integer quantity, Long eventId) {
        if (quantity == null || quantity <= 0) quantity = 1;

        Membership member = membershipRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int totalCost = product.getPricePoints() * quantity;

        Wallet userWallet = walletRepository.findByUser_UserId(member.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        if (userWallet.getOwnerType() != WalletOwnerTypeEnum.USER)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid wallet owner type");

        if (userWallet.getBalancePoints() < totalCost)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points");

        userWallet.setBalancePoints(userWallet.getBalancePoints() - totalCost);
        walletRepository.save(userWallet);

        Redeem redeem = Redeem.builder()
                .member(member)
                .event(eventId == null ? null : Event.builder().eventId(eventId).build())
                .product(product)
                .quantity(quantity)
                .totalCostPoints(totalCost)
                .status(RedeemStatusEnum.PENDING)
                .build();
        redeemRepository.save(redeem);

        ProductTransaction tx = ProductTransaction.builder()
                .product(product)
                .redeem(redeem)
                .quantity(quantity)
                .status(ProductTxStatusEnum.RESERVED)
                .build();
        productTxRepository.save(tx);

        return redeem;
    }
}
