package com.example.uniclub.service;

import com.example.uniclub.entity.StudentRegistry;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MajorRepository;
import com.example.uniclub.repository.StudentRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentRegistryService {

    private final StudentRegistryRepository registryRepo;
    private final MajorRepository majorRepo;

    // Format MSSV: {majorCode}{intake}{order}
    // VD: SE170458
    private static final String STUDENT_CODE_REGEX =
            "^[A-Z]{2}(1[5-9]|2[0-9])[0-9]{4}$";

    public StudentRegistry validate(String rawCode) {

        if (rawCode == null || rawCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Student code is required");
        }

        // Chuẩn hoá input
        String code = rawCode.trim().toUpperCase();

        // 1) Validate format chung
        if (!code.matches(STUDENT_CODE_REGEX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid student code format. Expected: XXYYZZZZ (e.g., SE170458)");
        }

        // 2) Validate major code tồn tại trong bảng majors
        String majorCode = code.substring(0, 2);

        if (!majorRepo.existsByMajorCode(majorCode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Major code does not exist: " + majorCode);
        }

        // 3) Validate MSSV có tồn tại trong registry hay không
        return registryRepo.findByStudentCode(code)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Student code not found in registry: " + code));
    }
}
