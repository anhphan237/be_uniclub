package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {

    boolean existsByName(String name);

    Optional<Club> findByLeader_UserId(Long userId);

    Optional<Club> findByClubId(Long id);

    // 🆕 Lọc các CLB active mà user chưa tham gia hoặc chờ duyệt
    @Query("""
        SELECT c
        FROM Club c
        WHERE c.activityStatus = com.example.uniclub.enums.ClubActivityStatusEnum.ACTIVE
          AND (COALESCE(:excludedIds, NULL) IS NULL OR c.clubId NOT IN :excludedIds)
          AND (:kw IS NULL OR :kw = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :kw, '%')))
        """)
    Page<Club> findAvailableForApply(
            @Param("excludedIds") List<Long> excludedIds,
            @Param("kw") String keyword,
            Pageable pageable
    );
    Page<Club> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    boolean existsByNameIgnoreCase(String name);

}
