package com.example.uniclub.service;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;

public interface MemberApplicationService {

    // ✅ Sinh viên nộp đơn
    MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req);

    // ✅ Duyệt / từ chối đơn
    MemberApplicationResponse updateStatusByEmail(String email, Long applicationId, MemberApplicationStatusUpdateRequest req);

    // ✅ Admin / Staff xem tất cả
    List<MemberApplicationResponse> findAll();

    // ✅ Xem theo email (student → của mình, leader → tất cả)
    List<MemberApplicationResponse> findApplicationsByEmail(String email);

    // ✅ Lấy danh sách đơn ứng tuyển theo CLB
    List<MemberApplicationResponse> getByClubId(CustomUserDetails principal, Long clubId);
}
