package com.example.uniclub.service;

import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

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
    String kickMember(CustomUserDetails principal, Long membershipId);

    String requestLeave(Long userId, Long clubId, String reason);

    String reviewLeaveRequest(Long requestId, Long approverId, String action);

    Map<String, Object> getMemberOverview(Long userId);

    boolean isActiveMember(Long userId, Long clubId);



}
