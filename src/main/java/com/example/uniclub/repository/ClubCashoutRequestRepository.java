package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.ClubCashoutRequest;
import com.example.uniclub.enums.CashoutStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubCashoutRequestRepository
        extends JpaRepository<ClubCashoutRequest, Long> {

    List<ClubCashoutRequest> findByClubOrderByRequestedAtDesc(Club club);

    List<ClubCashoutRequest> findByStatusOrderByRequestedAtAsc(CashoutStatusEnum status);

    Optional<ClubCashoutRequest> findByClubAndStatus(Club club, CashoutStatusEnum status);

    List<ClubCashoutRequest> findByClubAndStatusOrderByRequestedAtDesc(
            Club club,
            CashoutStatusEnum status
    );

    List<ClubCashoutRequest> findByStatusOrderByReviewedAtDesc(
            CashoutStatusEnum status
    );

    List<ClubCashoutRequest> findAllByOrderByRequestedAtDesc();
}
