package com.example.uniclub.service.impl;

import com.example.uniclub.entity.MultiplierPolicy;
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
    public List<MultiplierPolicy> getPolicies(PolicyTargetTypeEnum type) {
        return repo.findByTargetTypeOrderByMinEventsDesc(type);
    }

    @Override
    public MultiplierPolicy updatePolicy(Long id, Double newMultiplier, String updatedBy) {
        MultiplierPolicy p = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));
        p.setMultiplier(newMultiplier);
        p.setUpdatedBy(updatedBy);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    @Override
    public MultiplierPolicy createPolicy(MultiplierPolicy policy) {
        policy.setUpdatedAt(LocalDateTime.now());
        return repo.save(policy);
    }

    @Override
    public void deletePolicy(Long id) {
        repo.deleteById(id);
    }
}
