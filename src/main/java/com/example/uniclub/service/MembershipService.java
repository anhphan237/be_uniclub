package com.example.uniclub.service;

import com.example.uniclub.dto.request.MembershipCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;

public interface MembershipService {
    MembershipResponse create(MembershipCreateRequest req);
    void delete(Long id);
}
