package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubMonthlyActivity;
import com.example.uniclub.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubMonthlyActivityRepository extends JpaRepository<ClubMonthlyActivity, Long> {

    Optional<ClubMonthlyActivity> findByClubAndYearAndMonth(Club club, int year, int month);

    List<ClubMonthlyActivity> findByYearAndMonthOrderByFinalScoreDesc(int year, int month);
}
