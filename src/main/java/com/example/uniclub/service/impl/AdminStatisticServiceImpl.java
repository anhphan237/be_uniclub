package com.example.uniclub.service.impl;

import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.AdminStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatisticServiceImpl implements AdminStatisticService {

    private final UserRepository userRepo;

    @Override
    public List<Map<String, Object>> getStudentCountByMajor() {
        return userRepo.countStudentsByMajor().stream()
                .map(obj -> Map.of(
                        "majorName", obj[0],
                        "studentCount", obj[1]
                ))
                .toList();
    }
}
