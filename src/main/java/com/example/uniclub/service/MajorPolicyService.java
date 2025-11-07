package com.example.uniclub.service;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;
import com.example.uniclub.entity.MajorPolicy;

import java.util.List;

public interface MajorPolicyService {


    List<MajorPolicyResponse> getAll();

    MajorPolicyResponse getById(Long id);

    List<MajorPolicyResponse> getByMajor(Long majorId);

    MajorPolicyResponse create(MajorPolicyRequest req);

    MajorPolicyResponse update(Long id, MajorPolicyRequest req);

    void delete(Long id);
}
