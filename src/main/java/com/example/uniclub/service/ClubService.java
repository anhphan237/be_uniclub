package com.example.uniclub.service;

import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.request.ClubApplicationOfflineRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.ClubApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClubService {
    ClubResponse create(ClubCreateRequest req);
    ClubResponse get(Long id);
    Page<ClubResponse> list(Pageable pageable);
    void delete(Long id);
    Club saveClub(Club club);
    // 🆕 Dành cho ClubApplication
    void createFromOnlineApplication(ClubApplication app);
    void createFromOfflineApplication(ClubApplication app, ClubApplicationOfflineRequest req);
    void updateMemberCount(Long clubId);
}
