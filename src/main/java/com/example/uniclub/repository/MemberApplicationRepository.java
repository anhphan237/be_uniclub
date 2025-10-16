package com.example.uniclub.repository;

import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.MemberApplicationStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberApplicationRepository extends JpaRepository<MemberApplication, Long> {

    Page<MemberApplication> findByApplicant(User applicant, Pageable pageable);

    Page<MemberApplication> findByClub(Club club, Pageable pageable);

    Page<MemberApplication> findByClubAndStatusInAndCreatedAtBetween(
            Club club,
            List<MemberApplicationStatusEnum> statuses,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    long countByClubAndStatus(Club club, MemberApplicationStatusEnum status);

    List<MemberApplication> findByStatusAndCreatedAtBefore(
            MemberApplicationStatusEnum status, LocalDateTime deadline);
}
