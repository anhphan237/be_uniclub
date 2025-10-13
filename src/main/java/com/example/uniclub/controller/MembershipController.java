package com.example.uniclub.controller;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    // 🔹 Lấy danh sách CLB mà user đã tham gia
    @GetMapping("/my-clubs")
    public ResponseEntity<List<Membership>> getMyClubs(@AuthenticationPrincipal CustomUserDetails user) {
        List<Membership> memberships = membershipService.getMyMemberships(user.getId());
        return ResponseEntity.ok(memberships);
    }

    // 🔹 Kiểm tra xem user có là member của CLB cụ thể không
    @GetMapping("/check/{clubId}")
    public ResponseEntity<?> checkMembership(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isMember = membershipService.isMemberOfClub(user.getId(), clubId);
        return ResponseEntity.ok(Map.of("isMember", isMember));
    }
}
