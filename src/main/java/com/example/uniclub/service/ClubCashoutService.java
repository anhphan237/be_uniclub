package com.example.uniclub.service;

import com.example.uniclub.dto.response.CashoutResponse;
import com.example.uniclub.enums.CashoutStatusEnum;

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


    List<CashoutResponse> getCashoutsByClubAndStatus(
            Long clubId,
            CashoutStatusEnum status
    );

    CashoutResponse getCashoutDetail(Long id);

    List<CashoutResponse> getApprovedCashouts();

    List<CashoutResponse> getRejectedCashouts();

    List<CashoutResponse> getAllCashouts();
}
