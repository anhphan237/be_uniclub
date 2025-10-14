package com.example.uniclub.service;

import com.example.uniclub.dto.response.MembershipResponse;
import java.util.List;

public interface MembershipService {
    List<MembershipResponse> getMyMemberships(Long userId);
    boolean isMemberOfClub(Long userId, Long clubId);
}
