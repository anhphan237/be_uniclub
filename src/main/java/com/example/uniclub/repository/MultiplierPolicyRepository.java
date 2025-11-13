package com.example.uniclub.repository;

import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultiplierPolicyRepository extends JpaRepository<MultiplierPolicy, Long> {



    List<MultiplierPolicy> findByTargetTypeAndActivityTypeAndActiveTrue(
            PolicyTargetTypeEnum targetType,
            PolicyActivityTypeEnum activityType
    );

    Optional<MultiplierPolicy> findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
            PolicyTargetTypeEnum targetType,
            PolicyActivityTypeEnum activityType,
            String ruleName
    );

    // 🔹 Check duplicate policy
    boolean existsByTargetTypeAndActivityTypeAndRuleName(
            PolicyTargetTypeEnum targetType,
            PolicyActivityTypeEnum activityType,
            String ruleName
    );
    List<MultiplierPolicy> findByTargetTypeOrderByActivityTypeAscMinThresholdAsc(
            PolicyTargetTypeEnum targetType
    );
    List<MultiplierPolicy> findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
            PolicyTargetTypeEnum targetType,
            PolicyActivityTypeEnum activityType
    );


}
