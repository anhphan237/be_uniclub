package com.example.uniclub.repository;

import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultiplierPolicyRepository extends JpaRepository<MultiplierPolicy, Long> {

    // ✅ Lấy tất cả theo loại chính sách (theo field mới)
    List<MultiplierPolicy> findByTargetTypeOrderByMinEventsForClubDesc(PolicyTargetTypeEnum targetType);

    // ✅ Lấy tất cả active theo loại
    List<MultiplierPolicy> findByTargetTypeAndActiveTrueOrderByMinEventsForClubDesc(
            PolicyTargetTypeEnum targetType
    );

    // ✅ Lấy 1 policy cụ thể theo loại và levelOrStatus
    @Query("""
           SELECT p FROM MultiplierPolicy p 
           WHERE p.targetType = :targetType 
             AND p.levelOrStatus = :levelOrStatus 
             AND p.active = true
           """)
    Optional<MultiplierPolicy> findByTargetTypeAndLevelOrStatusAndActiveTrue(
            @Param("targetType") PolicyTargetTypeEnum targetType,
            @Param("levelOrStatus") String levelOrStatus
    );

    // ✅ Kiểm tra trùng lặp policy
    @Query("""
           SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END 
           FROM MultiplierPolicy p 
           WHERE p.targetType = :targetType 
             AND p.levelOrStatus = :levelOrStatus
           """)
    boolean existsByTargetTypeAndLevelOrStatus(
            @Param("targetType") PolicyTargetTypeEnum targetType,
            @Param("levelOrStatus") String levelOrStatus
    );
}
