package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Major;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MajorRepository;
import com.example.uniclub.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepo;

    @Override
    public List<Major> getAll() {
        return majorRepo.findAll();
    }

    @Override
    public Major create(Major major) {
        if (majorRepo.existsByName(major.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Major name already exists");
        }
        major.setActive(true);
        return majorRepo.save(major);
    }

    @Override
    public void delete(Long id) {
        if (!majorRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Major not found");
        }
        majorRepo.deleteById(id);
    }
}
