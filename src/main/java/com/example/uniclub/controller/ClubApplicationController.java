package com.example.uniclub.controller;

import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.enums.ApplicationStatusEnum;
import com.example.uniclub.mapper.ClubApplicationMapper;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.ClubApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/club-applications")
@RequiredArgsConstructor
public class ClubApplicationController {

    private final ClubApplicationService clubApplicationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ClubApplicationResponse createApplication(HttpServletRequest request,
                                                     @RequestParam String clubName,
                                                     @RequestParam(required = false) String description) {
        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");

        String email = jwtUtil.getSubject(token);

        ClubApplication clubApplication =
                clubApplicationService.createApplication(email, clubName, description);

        return ClubApplicationMapper.INSTANCE.toResponse(clubApplication);
    }

    // Xem tất cả đơn
    @GetMapping
    public List<ClubApplicationResponse> getAllApplications() {
        return clubApplicationService.getAllApplications()
                .stream()
                .map(ClubApplicationMapper.INSTANCE::toResponse)
                .toList();
    }

    // Xem đơn theo status (SUBMITTED/APPROVED/REJECTED)
    @GetMapping("/status/{status}")
    public List<ClubApplicationResponse> getApplicationsByStatus(@PathVariable ApplicationStatusEnum status) {
        return clubApplicationService.getApplicationsByStatus(status)
                .stream()
                .map(ClubApplicationMapper.INSTANCE::toResponse)
                .toList();
    }

    // Cập nhật trạng thái đơn (APPROVED / REJECTED)
    @PutMapping("/{id}/status")
    public ClubApplicationResponse updateApplicationStatus(@PathVariable Long id,
                                                           @RequestParam ApplicationStatusEnum status,
                                                           HttpServletRequest request) {
        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");
        String reviewerEmail = jwtUtil.getSubject(token);

        ClubApplication updatedApp = clubApplicationService.updateStatus(id, status, reviewerEmail);
        return ClubApplicationMapper.INSTANCE.toResponse(updatedApp);
    }
}

