package com.example.uniclub.service;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.dto.response.MemberApplicationStatsResponse;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;

public interface MemberApplicationService {


    MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req);
    MemberApplicationResponse updateStatusByEmail(String email, Long applicationId, MemberApplicationStatusUpdateRequest req);
    List<MemberApplicationResponse> findAll();
    List<MemberApplicationResponse> findApplicationsByEmail(String email);
    List<MemberApplicationResponse> getByClubId(CustomUserDetails principal, Long clubId);
    MemberApplicationResponse getApplicationById(CustomUserDetails principal, Long id);
    void cancelApplication(CustomUserDetails principal, Long id);
    List<MemberApplicationResponse> getPendingByClub(CustomUserDetails principal, Long clubId);
    MemberApplicationResponse approve(CustomUserDetails principal, Long id);
    MemberApplicationResponse reject(CustomUserDetails principal, Long id, String note);
    MemberApplicationStatsResponse getStatsByClub(Long clubId);
    MemberApplicationResponse updateNoteForApplication(CustomUserDetails principal, Long id, String note);
    List<MemberApplicationResponse> getApplicationsByStatus(String status);
    List<MemberApplicationResponse> getRecentApplications();
    List<MemberApplicationStatsResponse> getDailyStats(Long clubId);
    List<MemberApplicationResponse> getApplicationsByApplicant(Long userId);
    MemberApplicationResponse resubmitApplication(CustomUserDetails principal, Long id, MemberApplicationCreateRequest req);
    List<MemberApplicationResponse> getHandledApplications(CustomUserDetails principal, Long clubId);
}
