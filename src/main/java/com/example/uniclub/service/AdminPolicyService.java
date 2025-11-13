package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminPolicyResponse;
import java.util.List;

public interface AdminPolicyService {
    List<AdminPolicyResponse> getAllPolicies();
    AdminPolicyResponse getPolicyById(Long id);
    AdminPolicyResponse savePolicy(AdminPolicyResponse req, String updatedBy);
    AdminPolicyResponse updateMultiplier(Long id, Double newMultiplier, String updatedBy);
    void deletePolicy(Long id);

}
