package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiplierPolicyServiceImpl implements MultiplierPolicyService {

    private final MultiplierPolicyRepository multiplierPolicyRepository;
    private final MultiplierPolicyRepository multiplierRepo;

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

        boolean exists = multiplierPolicyRepository.existsByTargetTypeAndLevelOrStatus(
                req.getTargetType(), req.getLevelOrStatus());
        if (exists) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Policy already exists for this target type and level/status");
        }

        MultiplierPolicy policy = MultiplierPolicy.builder()
                .targetType(req.getTargetType())
                .levelOrStatus(req.getLevelOrStatus())
                .minEventsForClub(req.getMinEvents()) // ✅ đổi tên field
                .multiplier(req.getMultiplier())
                .updatedBy(req.getUpdatedBy())
                .updatedAt(LocalDateTime.now())
                .effectiveFrom(LocalDateTime.now())
                .active(req.isActive())
                .build();

        MultiplierPolicy saved = multiplierPolicyRepository.save(policy);
        return toResponse(saved);
    }

    // ================================================================
    // ✏️ Cập nhật
    // ================================================================
    @Override
    public MultiplierPolicyResponse update(Long id, MultiplierPolicyRequest req) {
        MultiplierPolicy existing = multiplierPolicyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));

        boolean duplicate = multiplierPolicyRepository.existsByTargetTypeAndLevelOrStatus(
                req.getTargetType(), req.getLevelOrStatus());
        if (duplicate && !req.getLevelOrStatus().equals(existing.getLevelOrStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A policy already exists for this target type and level/status");
        }

        existing.setTargetType(req.getTargetType());
        existing.setLevelOrStatus(req.getLevelOrStatus());
        existing.setMinEventsForClub(req.getMinEvents()); // ✅ đổi tên setter
        existing.setMultiplier(req.getMultiplier());
        existing.setUpdatedBy(req.getUpdatedBy());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setActive(req.isActive());

        MultiplierPolicy updated = multiplierPolicyRepository.save(existing);
        return toResponse(updated);
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
                .findByTargetTypeAndActiveTrueOrderByMinEventsForClubDesc(targetType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ================================================================
    // 🧮 Tìm multiplier theo cấp độ
    // ================================================================
    @Override
    public Double getMultiplierForLevel(PolicyTargetTypeEnum targetType, String levelOrStatus) {
        return multiplierPolicyRepository
                .findByTargetTypeAndLevelOrStatusAndActiveTrue(targetType, levelOrStatus)
                .map(MultiplierPolicy::getMultiplier)
                .orElse(1.0);
    }

    // ================================================================
    // 🔁 Convert Entity → DTO
    // ================================================================
    private MultiplierPolicyResponse toResponse(MultiplierPolicy entity) {
        return MultiplierPolicyResponse.builder()
                .id(entity.getId())
                .targetType(entity.getTargetType())
                .levelOrStatus(entity.getLevelOrStatus())
                .minEvents(entity.getMinEventsForClub()) // ✅ sửa ở đây
                .multiplier(entity.getMultiplier())
                .active(entity.isActive())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    // ================================================================
    // 🔹 Lấy toàn bộ theo loại (bất kể active)
    // ================================================================
    @Override
    public List<MultiplierPolicy> getPolicies(PolicyTargetTypeEnum type) {
        return multiplierPolicyRepository.findByTargetTypeOrderByMinEventsForClubDesc(type); // ✅ sửa field
    }

    @Override
    public Optional<MultiplierPolicy> findByTargetTypeAndLevelOrStatus(
            PolicyTargetTypeEnum targetType,
            String levelOrStatus
    ) {
        return multiplierRepo.findByTargetTypeAndLevelOrStatusAndActiveTrue(
                targetType,
                levelOrStatus
        );
    }

    @Override
    public List<MultiplierPolicy> getActiveEntityByTargetType(PolicyTargetTypeEnum targetType) {
        return multiplierPolicyRepository.findByTargetTypeAndActiveTrueOrderByMinEventsForClubDesc(targetType);
    }


}
