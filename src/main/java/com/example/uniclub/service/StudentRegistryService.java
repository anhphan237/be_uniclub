package com.example.uniclub.service;

import com.example.uniclub.entity.StudentRegistry;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MajorRepository;
import com.example.uniclub.repository.StudentRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentRegistryService {

    private final StudentRegistryRepository registryRepo;
    private final MajorRepository majorRepo;

    private static final String STUDENT_CODE_REGEX =
            "^[A-Z]{2}(1[5-9]|2[0-9])[0-9]{4}$";

    // ====================================================================
    // VALIDATE MSSV
    // ====================================================================
    public StudentRegistry validate(String rawCode) {
        if (rawCode == null || rawCode.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Student code is required");

        String code = rawCode.trim().toUpperCase();

        if (!code.matches(STUDENT_CODE_REGEX))
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid student code format. Expected: XXYYZZZZ");

        String majorCode = code.substring(0, 2);

        if (!majorRepo.existsByMajorCode(majorCode))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Major code does not exist: " + majorCode);

        return registryRepo.findByStudentCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Student code not found in registry: " + code));
    }

    // ====================================================================
    // IMPORT (CSV + EXCEL SUPPORT)
    // ====================================================================
    public Map<String, Object> importCsv(MultipartFile file) {

        if (file.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");

        String filename = file.getOriginalFilename();
        if (filename == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name");

        boolean isExcel = filename.endsWith(".xlsx") || filename.endsWith(".xls");

        return isExcel ? importExcel(file) : importCsvInternal(file);
    }

    // ====================================================================
    // 1) IMPORT CSV
    // ====================================================================
    private Map<String, Object> importCsvInternal(MultipartFile file) {
        int imported = 0, skipped = 0;
        List<StudentRegistry> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;

                if (lineNum == 1 && line.contains("student_code"))
                    continue;

                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split(",");

                if (parts.length < 2) {
                    skipped++;
                    continue;
                }

                String code = parts[0].trim().toUpperCase();
                String fullName = parts[1].trim();

                if (!validateRow(code, fullName)) {
                    skipped++;
                    continue;
                }

                batch.add(buildRegistry(code, fullName));
                imported++;
            }

            registryRepo.saveAll(batch);

        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading CSV: " + e.getMessage());
        }

        return Map.of("imported", imported, "skipped", skipped, "total", imported + skipped);
    }

    // ====================================================================
    // 2) IMPORT EXCEL (.xlsx) — CHỈ CẦN 2 CỘT: student_code, full_name
    // ====================================================================
    private Map<String, Object> importExcel(MultipartFile file) {

        int imported = 0, skipped = 0;
        List<StudentRegistry> batch = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {

            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            int rowNum = 0;

            for (Row row : sheet) {
                rowNum++;

                if (rowNum == 1) continue; // Skip header

                Cell codeCell = row.getCell(0);
                Cell fullNameCell = row.getCell(1);

                if (codeCell == null || fullNameCell == null) {
                    skipped++;
                    continue;
                }

                String code = codeCell.getStringCellValue().trim().toUpperCase();
                String fullName = fullNameCell.getStringCellValue().trim();

                if (!validateRow(code, fullName)) {
                    skipped++;
                    continue;
                }

                batch.add(buildRegistry(code, fullName));
                imported++;
            }

            registryRepo.saveAll(batch);

        } catch (Exception e) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error reading Excel: " + e.getMessage()
            );
        }

        return Map.of("imported", imported, "skipped", skipped, "total", imported + skipped);
    }

    // ====================================================================
    // VALIDATE 1 ROW (dùng chung CSV + Excel)
    // ====================================================================
    private boolean validateRow(String code, String fullName) {

        if (!code.matches(STUDENT_CODE_REGEX))
            return false;

        if (registryRepo.existsByStudentCode(code))
            return false;

        String majorCode = code.substring(0, 2);

        return majorRepo.existsByMajorCode(majorCode);
    }

    // ====================================================================
    // BUILD ENTITY
    // ====================================================================
    private StudentRegistry buildRegistry(String code, String fullName) {

        String majorCode = code.substring(0, 2);
        Integer intake = Integer.parseInt(code.substring(2, 4));
        String order = code.substring(4);

        return StudentRegistry.builder()
                .studentCode(code)
                .fullName(fullName)
                .majorCode(majorCode)
                .intake(intake)
                .orderNumber(order)
                .build();
    }

    // ====================================================================
    // GET ALL
    // ====================================================================
    public List<StudentRegistry> getAll() {
        return registryRepo.findAll();
    }
    public List<StudentRegistry> search(String keyword) {
        return registryRepo.searchByCodeOrName(keyword.toUpperCase(), "%" + keyword + "%");
    }
    public void delete(String code) {
        registryRepo.deleteByStudentCode(code);
    }
    public void clearAll() {
        registryRepo.deleteAll();
    }
    public Map<String, Object> stats() {
        List<StudentRegistry> all = registryRepo.findAll();

        Map<String, Long> majorCount = all.stream()
                .collect(Collectors.groupingBy(StudentRegistry::getMajorCode, Collectors.counting()));

        Map<Integer, Long> intakeCount = all.stream()
                .collect(Collectors.groupingBy(StudentRegistry::getIntake, Collectors.counting()));

        return Map.of(
                "total", all.size(),
                "byMajor", majorCount,
                "byIntake", intakeCount
        );
    }

}
