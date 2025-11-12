package com.example.uniclub.service;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import java.util.List;

public interface WalletTransactionService {
    List<WalletTransactionResponse> getUniToEventTransactions();
}

