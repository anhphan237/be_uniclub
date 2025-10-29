package com.example.uniclub.service;

import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import java.util.List;

public interface MultiplierPolicyService {
    List<MultiplierPolicy> getPolicies(PolicyTargetTypeEnum type);
    MultiplierPolicy updatePolicy(Long id, Double newMultiplier, String updatedBy);
    MultiplierPolicy createPolicy(MultiplierPolicy policy);
    void deletePolicy(Long id);
}
