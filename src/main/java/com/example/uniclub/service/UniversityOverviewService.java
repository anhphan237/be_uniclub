package com.example.uniclub.service;

import com.example.uniclub.dto.response.ClubOverviewResponse;

import java.util.List;

public interface UniversityOverviewService {

    // Tổng hợp toàn bộ thời gian
    List<ClubOverviewResponse> getAllClubOverview();

    // Tổng hợp theo tháng
    List<ClubOverviewResponse> getAllClubOverviewByMonth(int year, int month);

}
