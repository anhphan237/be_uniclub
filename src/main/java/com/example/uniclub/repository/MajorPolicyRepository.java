package com.example.uniclub.repository;

import com.example.uniclub.entity.MajorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MajorPolicyRepository extends JpaRepository<MajorPolicy, Long> {
    Optional<MajorPolicy> findByMajorIdAndActiveTrue(Long majorId);
}
