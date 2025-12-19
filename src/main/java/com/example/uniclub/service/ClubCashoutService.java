package com.example.uniclub.service;

import com.example.uniclub.dto.response.CashoutResponse;

import java.util.List;

public interface ClubCashoutService {

    CashoutResponse requestCashout(
            Long clubId,
            Long points,
            String leaderNote
    );

    void approveCashout(Long requestId, String staffNote);

    void rejectCashout(Long requestId, String staffNote);

    List<CashoutResponse> getCashoutsByClub(Long clubId);

    List<CashoutResponse> getPendingCashouts();
}
