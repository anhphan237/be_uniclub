package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminPolicyResponse;
import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MajorPolicyRepository;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import com.example.uniclub.service.AdminPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPolicyServiceImpl implements AdminPolicyService {

    private final MajorPolicyRepository majorRepo;
    private final MultiplierPolicyRepository multiRepo;

    @Override
    public List<AdminPolicyResponse> getAllPolicies() {
        List<AdminPolicyResponse> list = new ArrayList<>();
        majorRepo.findAll().forEach(mp -> list.add(AdminPolicyResponse.fromMajor(mp)));
        multiRepo.findAll().forEach(mp -> list.add(AdminPolicyResponse.fromMultiplier(mp)));
        return list;
    }

    @Override
    public AdminPolicyResponse savePolicy(AdminPolicyResponse req) {
        if (req.getType().equals("MAJOR")) {
            MajorPolicy entity = majorRepo.save(req.toMajorEntity());
            return AdminPolicyResponse.fromMajor(entity);
        } else if (req.getType().equals("MULTIPLIER")) {
            MultiplierPolicy entity = multiRepo.save(req.toMultiplierEntity());
            return AdminPolicyResponse.fromMultiplier(entity);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid policy type");
        }
    }

    @Override
    public void deletePolicy(Long id) {
        if (majorRepo.existsById(id)) majorRepo.deleteById(id);
        else if (multiRepo.existsById(id)) multiRepo.deleteById(id);
        else throw new ApiException(HttpStatus.NOT_FOUND, "Policy not found");
    }
}
