package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MajorPolicyRepository;
import com.example.uniclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepo;
    private final MajorPolicyRepository majorPolicyRepo; // ✅ Thêm dòng này

    // 🟦 Chuyển entity → response DTO
    private ClubResponse toResp(Club c) {
        return ClubResponse.builder()
                .id(c.getClubId())
                .name(c.getName())
                .description(c.getDescription())
                // ✅ Lấy tên policy nếu có
                .majorPolicyName(c.getMajorPolicy() != null ? c.getMajorPolicy().getPolicyName() : null)
                .build();
    }

    // 🟩 Tạo CLB mới
    @Override
    public ClubResponse create(ClubCreateRequest req) {
        if (clubRepo.existsByName(req.name())) {
            throw new ApiException(HttpStatus.CONFLICT, "Tên CLB đã tồn tại");
        }

        // ✅ Lấy entity MajorPolicy đầy đủ từ DB
        MajorPolicy majorPolicy = majorPolicyRepo.findById(req.majorPolicyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major Policy không tồn tại"));

        // ✅ Gán đầy đủ thông tin cho CLB
        Club club = Club.builder()
                .name(req.name())
                .description(req.description())
                .majorPolicy(majorPolicy)
                .build();

        Club saved = clubRepo.save(club);
        return toResp(saved);
    }

    // 🟦 Lấy CLB theo ID
    @Override
    public ClubResponse get(Long id) {
        return clubRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club không tồn tại"));
    }

    // 🟦 Phân trang danh sách CLB
    @Override
    public Page<ClubResponse> list(Pageable pageable) {
        return clubRepo.findAll(pageable).map(this::toResp);
    }

    // 🟥 Xóa CLB
    @Override
    public void delete(Long id) {
        if (!clubRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Club không tồn tại");
        }
        clubRepo.deleteById(id);
    }
}
