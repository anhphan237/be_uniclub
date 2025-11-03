package com.example.uniclub.service;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyTargetTypeEnum;

import java.util.List;

public interface MultiplierPolicyService {

    // 🔹 Lấy tất cả policy (cả CLUB & MEMBER)
    List<MultiplierPolicyResponse> getAll();

    // 🔹 Lấy chi tiết 1 policy
    MultiplierPolicyResponse getById(Long id);

    // 🔹 Tạo mới policy
    MultiplierPolicyResponse create(MultiplierPolicyRequest request);

    // 🔹 Cập nhật policy
    MultiplierPolicyResponse update(Long id, MultiplierPolicyRequest request);

    // 🔹 Xoá policy
    void delete(Long id);

    // 🔹 Lấy danh sách policy đang active theo loại (CLUB hoặc MEMBER)
    List<MultiplierPolicyResponse> getActiveByTargetType(PolicyTargetTypeEnum targetType);

    // 🔹 Lấy multiplier cho 1 cấp độ cụ thể (dùng khi thưởng điểm)
    Double getMultiplierForLevel(PolicyTargetTypeEnum targetType, String level);
    List<MultiplierPolicy> getPolicies(PolicyTargetTypeEnum type);

}
