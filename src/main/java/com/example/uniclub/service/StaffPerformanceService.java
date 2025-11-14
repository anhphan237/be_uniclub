package com.example.uniclub.service;

import com.example.uniclub.dto.request.StaffPerformanceRequest;
import com.example.uniclub.entity.StaffPerformance;
import com.example.uniclub.entity.User;

public interface StaffPerformanceService {

    StaffPerformance createStaffPerformance(Long clubId,
                                            StaffPerformanceRequest request,
                                            User createdBy);
}
