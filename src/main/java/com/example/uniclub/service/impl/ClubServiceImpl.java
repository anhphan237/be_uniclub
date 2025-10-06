package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
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

    private ClubResponse toResp(Club c) {
        return ClubResponse.builder()
                .id(c.getClubId())
                .name(c.getName())
                .description(c.getDescription())
                .majorPolicyName(c.getMajorPolicy() != null ? c.getMajorPolicy().getMajorName() : null)
                .build();
    }

    @Override
    public ClubResponse create(ClubCreateRequest req) {
        if (clubRepo.existsByName(req.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Tên CLB đã tồn tại");

        Club c = Club.builder()
                .name(req.name())
                .description(req.description())
                .majorPolicy(MajorPolicy.builder().id(req.majorPolicyId()).build())
                .build();

        return toResp(clubRepo.save(c));
    }

    @Override
    public ClubResponse get(Long id) {
        return clubRepo.findById(id).map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club không tồn tại"));
    }

    @Override
    public Page<ClubResponse> list(Pageable pageable) {
        return clubRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public void delete(Long id) {
        if (!clubRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Club không tồn tại");
        clubRepo.deleteById(id);
    }
}
