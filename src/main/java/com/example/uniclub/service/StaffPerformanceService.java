package com.example.uniclub.service;

import com.example.uniclub.dto.request.StaffPerformanceRequest;
import com.example.uniclub.dto.response.StaffPerformanceMonthlySummaryResponse;
import com.example.uniclub.entity.StaffPerformance;
import com.example.uniclub.entity.User;

import java.util.List;

public interface StaffPerformanceService {

    StaffPerformance createStaffPerformance(Long clubId,
                                            StaffPerformanceRequest request,
                                            User createdBy);

    List<StaffPerformance> getEvaluationsByEvent(Long eventId);

    StaffPerformanceMonthlySummaryResponse getClubStaffMonthlySummary(
            Long clubId, int year, int month
    );
}
