package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.enums.ClubApplicationStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;     // ✅ bổ sung import này
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubApplicationRepository extends JpaRepository<ClubApplication, Long> {

    // 🔹 Lấy danh sách theo trạng thái
    List<ClubApplication> findByStatus(ClubApplicationStatusEnum status);

    // 🔹 Tìm theo tên CLB (tránh trùng tên)
    Optional<ClubApplication> findByClubName(String clubName);

    // 🔹 Lấy danh sách đơn theo người nộp
    List<ClubApplication> findByProposer_UserId(Long userId);

    // 🔹 Đếm số đơn theo trạng thái
    long countByStatus(ClubApplicationStatusEnum status);

    // 🔹 Tìm kiếm theo tên CLB hoặc tên người nộp
    @Query("""
        SELECT a FROM ClubApplication a
        WHERE LOWER(a.clubName) LIKE LOWER(CONCAT('%', :kw, '%'))
           OR LOWER(a.proposer.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
    """)
    List<ClubApplication> searchByKeyword(@Param("kw") String keyword);

    Page<ClubApplication> findAll(Pageable pageable);

    List<ClubApplication> findAll();

    @Query("""
    SELECT a FROM ClubApplication a
    WHERE (:status IS NULL OR a.status = :status)
      AND (:clubType IS NULL OR a.clubType = :clubType)
      AND (:keyword IS NULL OR LOWER(a.clubName) LIKE LOWER(CONCAT('%', :keyword, '%')))
""")
    Page<ClubApplication> searchApplications(
            @Param("status") String status,
            @Param("clubType") String clubType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
