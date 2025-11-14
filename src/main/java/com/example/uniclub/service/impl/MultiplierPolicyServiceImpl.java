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

@Service
@RequiredArgsConstructor
public class MultiplierPolicyServiceImpl implements MultiplierPolicyService {

    private final MultiplierPolicyRepository repo;

    @Override
    public List<MultiplierPolicyResponse> getAll() {
        return repo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MultiplierPolicyResponse getById(Long id) {
        MultiplierPolicy p = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));
        return toResponse(p);
    }

    @Override
    public MultiplierPolicyResponse create(MultiplierPolicyRequest r) {

        boolean exists = repo.existsByTargetTypeAndActivityTypeAndRuleName(
                r.getTargetType(), r.getActivityType(), r.getRuleName()
        );

        if (exists) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Duplicate rule: same targetType + activityType + ruleName already exists.");
        }

        MultiplierPolicy p = MultiplierPolicy.builder()
                .targetType(r.getTargetType())
                .name(r.getName())
                .activityType(r.getActivityType())
                .ruleName(r.getRuleName())
                .conditionType(r.getConditionType())
                .minThreshold(r.getMinThreshold())
                .maxThreshold(r.getMaxThreshold())
                .multiplier(r.getMultiplier())
                .active(r.isActive())
                .updatedBy(r.getUpdatedBy())
                .policyDescription(r.getPolicyDescription())
                .updatedAt(LocalDateTime.now())
                .effectiveFrom(LocalDateTime.now())
                .build();

        return toResponse(repo.save(p));
    }

    @Override
    public MultiplierPolicyResponse update(Long id, MultiplierPolicyRequest r) {

        MultiplierPolicy old = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));

        boolean duplicate =
                repo.existsByTargetTypeAndActivityTypeAndRuleName(
                        r.getTargetType(), r.getActivityType(), r.getRuleName()
                );

        if (duplicate && !r.getRuleName().equals(old.getRuleName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A policy with the same ruleName already exists.");
        }

        old.setName(r.getName());
        old.setTargetType(r.getTargetType());
        old.setActivityType(r.getActivityType());
        old.setRuleName(r.getRuleName());
        old.setConditionType(r.getConditionType());
        old.setMinThreshold(r.getMinThreshold());
        old.setMaxThreshold(r.getMaxThreshold());
        old.setMultiplier(r.getMultiplier());
        old.setActive(r.isActive());
        old.setUpdatedBy(r.getUpdatedBy());
        old.setPolicyDescription(r.getPolicyDescription());
        old.setUpdatedAt(LocalDateTime.now());

        return toResponse(repo.save(old));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Policy not found");
        }
        repo.deleteById(id);
    }

    @Override
    public List<MultiplierPolicyResponse> getActiveByTargetType(PolicyTargetTypeEnum type) {
        return repo.findByTargetTypeOrderByActivityTypeAscMinThresholdAsc(type)
                .stream()
                .filter(MultiplierPolicy::isActive)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public double resolveMultiplier(PolicyTargetTypeEnum target,
                                    PolicyActivityTypeEnum activity,
                                    int value) {

        List<MultiplierPolicy> policies =
                repo.findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        target, activity
                );

        for (MultiplierPolicy p : policies) {
            boolean minOK = value >= p.getMinThreshold();
            boolean maxOK = (p.getMaxThreshold() == null) || value < p.getMaxThreshold();

            if (minOK && maxOK) return p.getMultiplier();
        }

        return 1.0;
    }

    private MultiplierPolicyResponse toResponse(MultiplierPolicy e) {
        return MultiplierPolicyResponse.builder()
                .id(e.getId())
                .targetType(e.getTargetType())
                .name(e.getName())
                .activityType(e.getActivityType())
                .ruleName(e.getRuleName())
                .conditionType(e.getConditionType())
                .minThreshold(e.getMinThreshold())
                .maxThreshold(e.getMaxThreshold())
                .multiplier(e.getMultiplier())
                .active(e.isActive())
                .updatedBy(e.getUpdatedBy())
                .policyDescription(e.getPolicyDescription())
                .build();
    }
}
