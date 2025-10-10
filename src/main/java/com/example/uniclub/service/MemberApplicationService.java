package com.example.uniclub.service;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import java.util.List;

public interface MemberApplicationService {

    // dùng email để xác định user
    MemberApplicationResponse createByEmail(String email, MemberApplicationCreateRequest req);

    MemberApplicationResponse updateStatusByEmail(String email, Long applicationId, MemberApplicationStatusUpdateRequest req);

    List<MemberApplicationResponse> findAll();

    List<MemberApplicationResponse> findApplicationsByEmail(String email);

    List<MemberApplicationResponse> getByClubId(Long clubId);

}
