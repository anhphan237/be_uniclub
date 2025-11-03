package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;
import com.example.uniclub.entity.Major;
import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MajorPolicyRepository;
import com.example.uniclub.repository.MajorRepository;
import com.example.uniclub.service.MajorPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
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
    // ➕ TẠO MỚI
    // ================================================================
    @Override
    public MajorPolicyResponse create(MajorPolicyRequest req) {
        Major major = majorRepo.findById(req.getMajorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));

        if (majorPolicyRepo.existsByMajor_Id(major.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "This major already has a policy");
        }

        MajorPolicy policy = MajorPolicy.builder()
                .policyName(req.getPolicyName())
                .description(req.getDescription())
                .major(major)
                .maxClubJoin(req.getMaxClubJoin())
                .active(req.isActive())
                .build();

        return toResponse(majorPolicyRepo.save(policy));
    }

    // ================================================================
    // ✏️ CẬP NHẬT
    // ================================================================
    @Override
    public MajorPolicyResponse update(Long id, MajorPolicyRequest req) {
        MajorPolicy existing = majorPolicyRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MajorPolicy not found"));

        Major major = majorRepo.findById(req.getMajorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));

        existing.setPolicyName(req.getPolicyName());
        existing.setDescription(req.getDescription());
        existing.setMajor(major);
        existing.setMaxClubJoin(req.getMaxClubJoin());
        existing.setActive(req.isActive());

        return toResponse(majorPolicyRepo.save(existing));
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
    // 📘 LẤY CHÍNH SÁCH ĐANG HOẠT ĐỘNG CỦA NGÀNH
    // ================================================================
    @Override
    public MajorPolicy getActivePolicyByMajor(Long majorId) {
        return majorPolicyRepo.findByMajor_IdAndActiveTrue(majorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active policy for this major"));
    }

    // ================================================================
    // 🔄 ENTITY → RESPONSE DTO
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
}
