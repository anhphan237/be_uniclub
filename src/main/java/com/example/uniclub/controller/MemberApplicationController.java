package com.example.uniclub.controller;

import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.MemberApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member-applications")
@RequiredArgsConstructor
public class MemberApplicationController {

    private final MemberApplicationService memberApplicationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public MemberApplication createApplication(HttpServletRequest request,
                                               @RequestParam Long clubId) {
        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");

        // Lấy email từ token
        String email = jwtUtil.getSubject(token);

        // Gọi service xử lý
        return memberApplicationService.createApplication(email, clubId);
    }
}
