package com.example.uniclub.service;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;

import java.util.List;

public interface MajorPolicyService {

    List<MajorPolicyResponse> getAll();

    MajorPolicyResponse getById(Long id);

    MajorPolicyResponse create(MajorPolicyRequest request);

    MajorPolicyResponse update(Long id, MajorPolicyRequest request);

    void delete(Long id);
}
