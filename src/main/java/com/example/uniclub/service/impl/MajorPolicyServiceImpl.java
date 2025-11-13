package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;
import com.example.uniclub.entity.Major;
import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MajorPolicyRepository;
import com.example.uniclub.repository.MajorRepository;
import com.example.uniclub.service.MajorPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MajorPolicyServiceImpl implements MajorPolicyService {

    private final MajorPolicyRepository majorPolicyRepo;
    private final MajorRepository majorRepo;

    // ================================================================
    // 🧾 LẤY TẤT CẢ
    // ================================================================
    @Override
    public List<MajorPolicyResponse> getAll() {
        return majorPolicyRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // 🔍 LẤY THEO ID
    // ================================================================
    @Override
    public MajorPolicyResponse getById(Long id) {
        MajorPolicy policy = majorPolicyRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MajorPolicy not found"));
        return toResponse(policy);
    }

    // ================================================================
    // 🔍 LẤY TẤT CẢ POLICY THEO MAJOR ID
    // ================================================================
    @Override
    public List<MajorPolicyResponse> getByMajor(Long majorId) {
        List<MajorPolicy> policies = majorPolicyRepo.findByMajor_Id(majorId);
        if (policies.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No policies found for this major");
        }
        return policies.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ================================================================
    // 🟩 LẤY POLICY ACTIVE THEO MAJOR (ĐANG DÙNG)
    // ================================================================
    @Override
    public List<MajorPolicyResponse> getActiveByMajor(Long majorId) {
        List<MajorPolicy> activePolicies = majorPolicyRepo.findByMajor_IdAndActiveTrue(majorId);
        if (activePolicies.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No active policies for this major");
        }
        return activePolicies.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ================================================================
    // ➕ TẠO MỚI — CHỐNG TRÙNG MAJOR POLICY
    // ================================================================
    @Override
    @Transactional
    public MajorPolicyResponse create(MajorPolicyRequest req) {
        Major major = majorRepo.findById(req.getMajorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));

        // ✅ Chỉ cho phép 1 policy/major (hoặc điều kiện tùy bạn)
        if (majorPolicyRepo.existsByMajor_Id(req.getMajorId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A policy already exists for this major. Please update the existing one instead.");
        }

        MajorPolicy policy = MajorPolicy.builder()
                .policyName(req.getPolicyName())
                .description(req.getDescription())
                .major(major)
                .maxClubJoin(req.getMaxClubJoin())
                .active(req.isActive())
                .build();

        MajorPolicy saved = majorPolicyRepo.save(policy);
        return toResponse(saved);
    }

    // ================================================================
    // ✏️ CẬP NHẬT
    // ================================================================
    @Override
    public MajorPolicyResponse update(Long id, MajorPolicyRequest req) {
        MajorPolicy existing = majorPolicyRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MajorPolicy not found"));

        existing.setPolicyName(req.getPolicyName());
        existing.setDescription(req.getDescription());
        existing.setMaxClubJoin(req.getMaxClubJoin());
        existing.setActive(req.isActive());

        MajorPolicy updated = majorPolicyRepo.save(existing);
        return toResponse(updated);
    }

    // ================================================================
    // ❌ XOÁ
    // ================================================================
    @Override
    public void delete(Long id) {
        if (!majorPolicyRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MajorPolicy not found");
        }
        majorPolicyRepo.deleteById(id);
    }

    // ================================================================
    // 🔁 ENTITY → RESPONSE DTO
    // ================================================================
    private MajorPolicyResponse toResponse(MajorPolicy entity) {
        return MajorPolicyResponse.builder()
                .id(entity.getId())
                .policyName(entity.getPolicyName())
                .description(entity.getDescription())
                .majorId(entity.getMajor() != null ? entity.getMajor().getId() : null)
                .majorName(entity.getMajor() != null ? entity.getMajor().getName() : null)
                .maxClubJoin(entity.getMaxClubJoin())
                .active(entity.isActive())
                .build();
    }
    @Override
    public void validateClubJoinLimit(User user, int currentJoinedClubs) {
        if (user.getMajor() == null) {
            // Không có major -> không áp hạn mức
            return;
        }

        Long majorId = user.getMajor().getId();

        List<MajorPolicy> activePolicies =
                majorPolicyRepo.findByMajor_IdAndActiveTrue(majorId);

        // Không có policy active -> không giới hạn
        Integer maxJoin = activePolicies.stream()
                .map(MajorPolicy::getMaxClubJoin)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (maxJoin == null) {
            return; // Không có limit -> cho qua
        }

        if (currentJoinedClubs >= maxJoin) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    String.format(
                            "You have reached the maximum number of clubs for your major. Allowed: %d, current: %d.",
                            maxJoin, currentJoinedClubs
                    ));
        }
    }
}
