package com.example.uniclub.service;

import com.example.uniclub.dto.response.ClubActivityMonthlyResponse;
import com.example.uniclub.dto.response.ClubActivityRankingItemResponse;
import com.example.uniclub.dto.response.MemberActivityDetailResponse;

import java.time.YearMonth;
import java.util.List;

public interface MemberActivityQueryService {

    ClubActivityMonthlyResponse getClubActivity(Long clubId, YearMonth month);

    MemberActivityDetailResponse getMemberActivity(Long membershipId, YearMonth month);

    List<ClubActivityRankingItemResponse> getClubRanking(YearMonth month);
}
