package com.example.uniclub.service;

import com.example.uniclub.entity.Membership;
import java.util.List;

public interface MembershipService {

    List<Membership> getMyMemberships(Long userId);

    boolean isMemberOfClub(Long userId, Long clubId);
}
