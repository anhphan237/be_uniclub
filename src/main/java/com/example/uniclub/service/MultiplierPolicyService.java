package com.example.uniclub.service;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;

import java.util.List;

public interface MultiplierPolicyService {

    List<MultiplierPolicyResponse> getAll();

    MultiplierPolicyResponse getById(Long id);

    MultiplierPolicyResponse create(MultiplierPolicyRequest req);

    MultiplierPolicyResponse update(Long id, MultiplierPolicyRequest req);

    void delete(Long id);

    List<MultiplierPolicyResponse> getActiveByTargetType(PolicyTargetTypeEnum targetType);

    // 🔥 Hàm quan trọng nhất
    double resolveMultiplier(
            PolicyTargetTypeEnum target,
            PolicyActivityTypeEnum activity,
            int value
    );
}
