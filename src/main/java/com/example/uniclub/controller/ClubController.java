package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;

    // 🟢 1. Tạo CLB mới
    @PostMapping
    public ResponseEntity<ApiResponse<ClubResponse>> create(@Valid @RequestBody ClubCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.create(req)));
    }

    // 🔵 2. Lấy thông tin chi tiết 1 CLB (có memberCount)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubResponse>> get(@PathVariable Long id) {
        ClubResponse club = clubService.get(id);
        long memberCount = membershipRepo.countByClub_ClubIdAndState(id, MembershipStateEnum.ACTIVE);
        club.setMemberCount(memberCount); 
        return ResponseEntity.ok(ApiResponse.ok(club));
    }

    // 🟣 3. Lấy danh sách CLB (phân trang)
    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(clubService.list(pageable));
    }

    // 🔴 4. Xoá CLB (chỉ dành cho admin)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        clubService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // 🟡 5. Thống kê toàn hệ thống CLB
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClubs", clubRepo.count());
        stats.put("totalMembers", membershipRepo.count());
        stats.put("activeMembers", membershipRepo.countByState(MembershipStateEnum.ACTIVE));
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // 🧩 6. ✅ Lấy tổng số thành viên ACTIVE trong 1 CLB
    @GetMapping("/{id}/member-count")
//    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','CLUB_VICE_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberCount(@PathVariable Long id) {
        long count = membershipRepo.countByClub_ClubIdAndState(id, MembershipStateEnum.ACTIVE);
        Map<String, Object> result = new HashMap<>();
        result.put("clubId", id);
        result.put("activeMemberCount", count);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
