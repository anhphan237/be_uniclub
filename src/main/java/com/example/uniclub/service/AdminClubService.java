package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminClubResponse;
import com.example.uniclub.dto.response.AdminClubStatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminClubService {
    Page<AdminClubResponse> getAllClubs(String keyword, Pageable pageable);
    AdminClubResponse getClubDetail(Long id);
    void approveClub(Long id);
    void suspendClub(Long id);
    AdminClubStatResponse getClubStats(Long clubId);

}
