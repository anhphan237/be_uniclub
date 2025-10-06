package com.example.uniclub.service.impl;

import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.repository.MajorPolicyRepository;
import com.example.uniclub.service.MajorPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajorPolicyServiceImpl implements MajorPolicyService {

    private final MajorPolicyRepository majorPolicyRepository;

    @Override
    public List<MajorPolicy> getAll() {
        return majorPolicyRepository.findAll();
    }

    @Override
    public MajorPolicy getById(Long id) {
        return majorPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MajorPolicy not found"));
    }

    @Override
    public MajorPolicy create(MajorPolicy policy) {
        return majorPolicyRepository.save(policy);
    }

    @Override
    public MajorPolicy update(Long id, MajorPolicy updated) {
        MajorPolicy existing = getById(id);
        existing.setMajorName(updated.getMajorName());
        existing.setDescription(updated.getDescription());
        existing.setMaxClubJoin(updated.getMaxClubJoin());
        existing.setRewardMultiplier(updated.getRewardMultiplier());
        existing.setActive(updated.isActive());
        return majorPolicyRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        majorPolicyRepository.deleteById(id);
    }
}
