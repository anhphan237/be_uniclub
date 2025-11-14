package com.example.uniclub.service;

import com.example.uniclub.dto.response.MemberRewardSuggestionResponse;

import java.util.List;

public interface RewardSuggestionService {
    List<MemberRewardSuggestionResponse> getRewardSuggestions(Long clubId, int year, int month);
}
