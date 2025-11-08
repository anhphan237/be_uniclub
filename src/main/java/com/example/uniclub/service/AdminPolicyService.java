package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminPolicyResponse;
import java.util.List;

public interface AdminPolicyService {
    List<AdminPolicyResponse> getAllPolicies();
    AdminPolicyResponse savePolicy(AdminPolicyResponse req);
    void deletePolicy(Long id);
}
