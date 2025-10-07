package com.example.uniclub.service;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.enums.ApplicationStatusEnum;

import java.util.List;

public interface ClubApplicationService {
    ClubApplication createApplication(String email, String clubName, String description);
    List<ClubApplication> getAllApplications();
    List<ClubApplication> getApplicationsByStatus(ApplicationStatusEnum status);
}
