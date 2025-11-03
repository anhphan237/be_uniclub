package com.example.uniclub.service;

import com.example.uniclub.dto.response.UserStatsResponse;

public interface UserStatsService {
    UserStatsResponse getUserStats(Long userId);
}
