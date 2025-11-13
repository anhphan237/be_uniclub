package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import com.example.uniclub.service.MultiplierPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiplierPolicyServiceImpl implements MultiplierPolicyService {

    private final MultiplierPolicyRepository multiplierPolicyRepository;

    // ================================================================
    // 🧾 Lấy tất cả
    // ================================================================
    @Override
    public List<MultiplierPolicyResponse> getAll() {
        return multiplierPolicyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // 🔍 Lấy theo ID
    // ================================================================
    @Override
    public MultiplierPolicyResponse getById(Long id) {
        MultiplierPolicy policy = multiplierPolicyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));
        return toResponse(policy);
    }

    // ================================================================
    // ➕ Tạo mới
    // ================================================================
    @Override
    public MultiplierPolicyResponse create(MultiplierPolicyRequest req) {

        // ❗ Kiểm tra trùng rule theo targetType + activityType + ruleName
        boolean exists = multiplierPolicyRepository.existsByTargetTypeAndActivityTypeAndRuleName(
                req.getTargetType(),
                req.getActivityType(),
                req.getRuleName()
        );

        if (exists) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A policy with the same targetType, activityType and ruleName already exists.");
        }

        MultiplierPolicy policy = MultiplierPolicy.builder()
                .targetType(req.getTargetType())
                .activityType(req.getActivityType())
                .ruleName(req.getRuleName())
                .conditionType(req.getConditionType())
                .minThreshold(req.getMinThreshold())
                .maxThreshold(req.getMaxThreshold())
                .multiplier(req.getMultiplier())
                .active(req.isActive())
                .updatedBy(req.getUpdatedBy())
                .updatedAt(LocalDateTime.now())
                .effectiveFrom(LocalDateTime.now())
                .policyDescription(req.getPolicyDescription())
                .build();

        return toResponse(multiplierPolicyRepository.save(policy));
    }

    // ================================================================
    // ✏️ Cập nhật
    // ================================================================
    @Override
    public MultiplierPolicyResponse update(Long id, MultiplierPolicyRequest req) {

        MultiplierPolicy existing = multiplierPolicyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));

        // Kiểm tra rule trùng (nếu đổi ruleName hoặc activityType)
        boolean duplicate = multiplierPolicyRepository.existsByTargetTypeAndActivityTypeAndRuleName(
                req.getTargetType(), req.getActivityType(), req.getRuleName()
        );

        if (duplicate && !req.getRuleName().equals(existing.getRuleName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A policy with the same targetType, activityType and ruleName already exists.");
        }

        existing.setTargetType(req.getTargetType());
        existing.setActivityType(req.getActivityType());
        existing.setRuleName(req.getRuleName());
        existing.setConditionType(req.getConditionType());
        existing.setMinThreshold(req.getMinThreshold());
        existing.setMaxThreshold(req.getMaxThreshold());
        existing.setMultiplier(req.getMultiplier());
        existing.setActive(req.isActive());
        existing.setUpdatedBy(req.getUpdatedBy());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setPolicyDescription(req.getPolicyDescription());

        return toResponse(multiplierPolicyRepository.save(existing));
    }

    // ================================================================
    // ❌ Xóa
    // ================================================================
    @Override
    public void delete(Long id) {
        if (!multiplierPolicyRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Policy not found");
        }
        multiplierPolicyRepository.deleteById(id);
    }

    // ================================================================
    // 🟩 Lấy danh sách policy active theo loại
    // ================================================================
    @Override
    public List<MultiplierPolicyResponse> getActiveByTargetType(PolicyTargetTypeEnum targetType) {
        return multiplierPolicyRepository
                .findByTargetTypeOrderByActivityTypeAscMinThresholdAsc(targetType)
                .stream()
                .filter(MultiplierPolicy::isActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // 🔁 Convert Entity → DTO
    // ================================================================
    private MultiplierPolicyResponse toResponse(MultiplierPolicy entity) {
        return MultiplierPolicyResponse.builder()
                .id(entity.getId())
                .targetType(entity.getTargetType())
                .activityType(entity.getActivityType())
                .ruleName(entity.getRuleName())
                .conditionType(entity.getConditionType())
                .minThreshold(entity.getMinThreshold())
                .maxThreshold(entity.getMaxThreshold())
                .multiplier(entity.getMultiplier())
                .active(entity.isActive())
                .updatedBy(entity.getUpdatedBy())
                .policyDescription(entity.getPolicyDescription())
                .build();
    }

    // ================================================================
    // 🧮 Áp dụng multiplier theo giá trị hoạt động (KHÔNG hard-code)
    // ================================================================
    @Override
    public double resolveMultiplier(
            PolicyTargetTypeEnum target,
            PolicyActivityTypeEnum activity,
            int value
    ) {
        List<MultiplierPolicy> policies =
                multiplierPolicyRepository.findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        target, activity
                );

        for (MultiplierPolicy p : policies) {
            boolean minOK = value >= p.getMinThreshold();
            boolean maxOK = (p.getMaxThreshold() == null) || value < p.getMaxThreshold();

            if (minOK && maxOK) {
                return p.getMultiplier();  // 🔥 lấy multiplier từ DB → unistaff kiểm soát 100%
            }
        }
        return 1.0; // default multiplier
    }
}
