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

    private final MajorPolicyRepository majorPolicyRepository;
    private final MajorRepository majorRepository;
    private final MajorPolicyRepository majorPolicyRepo;
    // ✅ Lấy tất cả
    @Override
    public List<MajorPolicyResponse> getAll() {
        return majorPolicyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ✅ Lấy theo ID
    @Override
    public MajorPolicyResponse getById(Long id) {
        MajorPolicy policy = majorPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MajorPolicy not found"));
        return toResponse(policy);
    }

    // ✅ Tạo mới
    @Override
    public MajorPolicyResponse create(MajorPolicyRequest request) {
        MajorPolicy policy = new MajorPolicy();
        policy.setPolicyName(request.getPolicyName());
        policy.setDescription(request.getDescription());
        policy.setMajorId(request.getMajorId());

        policy.setActive(true);

        // ⚙️ Gán mặc định tránh lỗi null
        policy.setMaxClubJoin(request.getMaxClubJoin() != null ? request.getMaxClubJoin() : 3);
        policy.setRewardMultiplier(request.getRewardMultiplier() != null ? request.getRewardMultiplier() : 1.0);

        // ⚙️ Lấy tên ngành từ MajorRepository
        String majorName = majorRepository.findById(request.getMajorId())
                .map(Major::getName)
                .orElse("Default Major");
        policy.setMajorName(majorName);

        MajorPolicy saved = majorPolicyRepository.save(policy);
        return toResponse(saved);
    }

    // ✅ Cập nhật
    @Override
    public MajorPolicyResponse update(Long id, MajorPolicyRequest request) {
        MajorPolicy existing = majorPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MajorPolicy not found"));

        existing.setPolicyName(request.getPolicyName());
        existing.setDescription(request.getDescription());
        existing.setMajorId(request.getMajorId());

        existing.setActive(true);
        existing.setMaxClubJoin(request.getMaxClubJoin() != null ? request.getMaxClubJoin() : existing.getMaxClubJoin());
        existing.setRewardMultiplier(request.getRewardMultiplier() != null ? request.getRewardMultiplier() : existing.getRewardMultiplier());

        // cập nhật major name
        String majorName = majorRepository.findById(request.getMajorId())
                .map(Major::getName)
                .orElse(existing.getMajorName());
        existing.setMajorName(majorName);

        MajorPolicy updated = majorPolicyRepository.save(existing);
        return toResponse(updated);
    }

    // ✅ Xóa
    @Override
    public void delete(Long id) {
        if (!majorPolicyRepository.existsById(id)) {
            throw new RuntimeException("MajorPolicy not found");
        }
        majorPolicyRepository.deleteById(id);
    }

    // ✅ Chuyển sang DTO Response
    private MajorPolicyResponse toResponse(MajorPolicy entity) {
        return MajorPolicyResponse.builder()
                .id(entity.getId())
                .policyName(entity.getPolicyName())
                .description(entity.getDescription())
                .majorId(entity.getMajorId())
                .majorName(entity.getMajorName()) // ✅ bây giờ builder nhận được rồi
                .maxClubJoin(entity.getMaxClubJoin())
                .rewardMultiplier(entity.getRewardMultiplier())
                .active(entity.isActive())
                .build();
    }
    @Override
    public MajorPolicy getActivePolicyByMajor(Long majorId) {
        return majorPolicyRepo.findByMajorIdAndActiveTrue(majorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active policy for this major"));
    }

    @Override
    public double getRewardMultiplierForMajor(Long majorId) {
        return majorPolicyRepo.findByMajorIdAndActiveTrue(majorId)
                .map(MajorPolicy::getRewardMultiplier)
                .orElse(1.0); // default multiplier = 1.0 (no change)
    }

}
