package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminPolicyResponse;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import com.example.uniclub.service.AdminPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPolicyServiceImpl implements AdminPolicyService {

    private final MultiplierPolicyRepository policyRepo;

    @Override
    public List<AdminPolicyResponse> getAllPolicies() {
        return policyRepo.findAll()
                .stream()
                .map(AdminPolicyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public AdminPolicyResponse savePolicy(AdminPolicyResponse req) {
        // cập nhật thời gian và người sửa gần nhất
        var entity = req.toEntity();
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getUpdatedBy() == null)
            entity.setUpdatedBy("unistaff@fpt.edu.vn");

        MultiplierPolicy saved = policyRepo.save(entity);
        return AdminPolicyResponse.fromEntity(saved);
    }

    @Override
    public void deletePolicy(Long id) {
        if (!policyRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Policy not found");
        policyRepo.deleteById(id);
    }
    @Override
    public AdminPolicyResponse updateMultiplier(Long id, Double newMultiplier, String updatedBy) {
        MultiplierPolicy policy = policyRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));

        if (newMultiplier == null || newMultiplier <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Multiplier must be > 0");

        policy.setMultiplier(newMultiplier);
        policy.setUpdatedAt(LocalDateTime.now());
        policy.setUpdatedBy(updatedBy);

        MultiplierPolicy saved = policyRepo.save(policy);
        return AdminPolicyResponse.fromEntity(saved);
    }
    @Override
    public AdminPolicyResponse getPolicyById(Long id) {
        var policy = policyRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));
        return AdminPolicyResponse.fromEntity(policy);
    }

}
