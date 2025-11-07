package com.example.uniclub.repository;

import com.example.uniclub.entity.MajorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorPolicyRepository extends JpaRepository<MajorPolicy, Long> {

    boolean existsByMajor_Id(Long majorId);

    List<MajorPolicy> findByMajor_Id(Long majorId);

    List<MajorPolicy> findByMajor_IdAndActiveTrue(Long majorId);
}
