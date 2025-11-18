package com.example.uniclub.service;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationListResponse;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.entity.User;

import java.util.List;
import java.util.Map;

public interface ClubApplicationService {

    ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req);

    ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req);

    ApiResponse<?> createClubAccounts(CreateClubAccountsRequest req);

    List<ClubApplicationListResponse> getPending();

    List<ClubApplicationListResponse> getByUser(Long userId);

    ClubApplicationResponse getById(Long userId, String roleName, Long id);

    void delete(Long id);

    Map<String, Object> getStatistics();

    List<ClubApplicationListResponse> search(String keyword);

    List<ClubApplicationListResponse> getAllApplications();

    void saveOtp(String email, String otp);

    void verifyOtp(String email, String otp);

    User findStudentByEmail(String email);
}
