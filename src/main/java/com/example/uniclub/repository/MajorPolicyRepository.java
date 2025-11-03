package com.example.uniclub.repository;

import com.example.uniclub.entity.MajorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MajorPolicyRepository extends JpaRepository<MajorPolicy, Long> {

    // 🔍 Lấy policy đang active của 1 ngành (Major)
    Optional<MajorPolicy> findByMajor_IdAndActiveTrue(Long majorId);

    // ✅ Kiểm tra xem ngành đã có policy chưa (để chặn trùng)
    boolean existsByMajor_Id(Long majorId);
}
