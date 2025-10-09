package com.example.uniclub.service;

import com.example.uniclub.dto.request.MemberCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;

public interface MembershipService {
    MembershipResponse create(MemberCreateRequest req);
    void delete(Long id);
}
