package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.enums.ClubApplicationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubApplicationRepository extends JpaRepository<ClubApplication, Long> {
    List<ClubApplication> findByStatus(ClubApplicationStatusEnum status);
    Optional<ClubApplication> findByClubName(String clubName);
    List<ClubApplication> findByProposer_UserId(Long userId);
    long countByStatus(ClubApplicationStatusEnum status);
    @Query("SELECT a FROM ClubApplication a WHERE LOWER(a.clubName) LIKE LOWER(CONCAT('%', :kw, '%')) " +
            "OR LOWER(a.proposer.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<ClubApplication> searchByKeyword(@Param("kw") String keyword);

}
