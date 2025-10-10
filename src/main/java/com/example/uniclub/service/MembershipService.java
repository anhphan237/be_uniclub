package com.example.uniclub.service;

import com.example.uniclub.dto.request.MemberCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;

public interface MembershipService {

    MembershipResponse create(MemberCreateRequest req);

    void delete(Long id);

    // 🔹 Leader xem member theo clubId (chỉ xem CLB của mình)
    List<MembershipResponse> getMembersByClub(CustomUserDetails principal, Long clubId);

    // 🔹 Leader xem member CLB mình sở hữu (khỏi truyền clubId)
    List<MembershipResponse> getMembersOfMyClub(CustomUserDetails principal);

    // (đã có từ trước) bật/tắt staff
    String updateStaffStatus(CustomUserDetails principal, Long membershipId, boolean value);
}
