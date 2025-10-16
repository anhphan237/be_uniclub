package com.example.uniclub.service;

import com.example.uniclub.dto.request.ClubApplicationCreateRequest;
import com.example.uniclub.dto.request.ClubApplicationOfflineRequest;
import com.example.uniclub.dto.request.ClubApplicationDecisionRequest;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import java.util.List;

public interface ClubApplicationService {
    ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req);
    ClubApplicationResponse createOffline(Long staffId, ClubApplicationOfflineRequest req);
    List<ClubApplicationResponse> getPending();
    ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req);
}
