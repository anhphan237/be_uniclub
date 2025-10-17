package com.example.uniclub.service;

import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;

import java.util.List;

public interface MembershipService {

    List<MembershipResponse> getMyMemberships(Long userId);

    boolean isMemberOfClub(Long userId, Long clubId);

    MembershipResponse joinClub(Long userId, Long clubId);

    List<MembershipResponse> getPendingMembers(Long clubId);

    MembershipResponse approveMember(Long membershipId, Long approverId);

    MembershipResponse rejectMember(Long membershipId, Long approverId, String reason);

    void removeMember(Long membershipId, Long approverId);


    MembershipResponse updateClubRole(Long membershipId, ClubRoleEnum newRole, Long approverId);

    List<MembershipResponse> getMembersByClub(Long clubId);

    List<MembershipResponse> getStaffMembers(Long clubId);

    List<MembershipResponse> getMembersByLeaderName(String leaderName);

    MembershipResponse updateRole(Long membershipId, Long approverId, String newRole);





}
