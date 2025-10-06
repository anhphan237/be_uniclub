package com.example.uniclub.service;

import com.example.uniclub.entity.MajorPolicy;
import java.util.List;

public interface MajorPolicyService {
    List<MajorPolicy> getAll();
    MajorPolicy getById(Long id);
    MajorPolicy create(MajorPolicy policy);
    MajorPolicy update(Long id, MajorPolicy updated);
    void delete(Long id);
}
