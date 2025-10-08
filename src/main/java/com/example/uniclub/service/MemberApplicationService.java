package com.example.uniclub.service;

import com.example.uniclub.entity.MemberApplication;

public interface MemberApplicationService {
    MemberApplication createApplication(String email, Long clubId);
}
