package com.example.uniclub.repository;

import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MultiplierPolicyRepository extends JpaRepository<MultiplierPolicy, Long> {
    List<MultiplierPolicy> findByTargetTypeOrderByMinEventsDesc(PolicyTargetTypeEnum type);
}
