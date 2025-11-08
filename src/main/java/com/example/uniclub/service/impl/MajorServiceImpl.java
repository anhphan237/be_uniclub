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
    public Major getById(Long id) {
        return majorRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));
    }

    @Override
    public Major getByMajorCode(String code) {
        return majorRepo.findByMajorCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major code not found"));
    }

    @Override
    public Major create(Major major) {
        if (majorRepo.existsByName(major.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Major name already exists");
        }
        if (majorRepo.existsByMajorCode(major.getMajorCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Major code already exists");
        }
        major.setActive(true);

        // ✅ Mặc định nếu chưa có colorHex, gán màu ngẫu nhiên hoặc giá trị mặc định
        if (major.getColorHex() == null || major.getColorHex().isBlank()) {
            major.setColorHex("#" + Integer.toHexString((int)(Math.random() * 0xFFFFFF)).toUpperCase());
        }

        return majorRepo.save(major);
    }

    @Override
    public Major update(Long id, Major updatedMajor) {
        Major existing = getById(id);
        existing.setName(updatedMajor.getName());
        existing.setDescription(updatedMajor.getDescription());
        existing.setMajorCode(updatedMajor.getMajorCode());
        existing.setActive(updatedMajor.isActive());

        // ✅ Cập nhật màu hex nếu có
        if (updatedMajor.getColorHex() != null && !updatedMajor.getColorHex().isBlank()) {
            existing.setColorHex(updatedMajor.getColorHex());
        }

        return majorRepo.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!majorRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Major not found");
        }
        majorRepo.deleteById(id);
    }
}
