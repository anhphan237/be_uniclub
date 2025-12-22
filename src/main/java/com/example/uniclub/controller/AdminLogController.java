package com.example.uniclub.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    @Operation(summary = "Xem 100 dòng lỗi gần nhất từ file log")
    @GetMapping("/errors")
    public ResponseEntity<List<String>> getRecentErrors() throws IOException {
        Path logPath = Paths.get("logs/app.log");
        if (!Files.exists(logPath)) {
            return ResponseEntity.ok(List.of("No log file found"));
        }
        List<String> allLines = Files.readAllLines(logPath);
        int size = allLines.size();
        return ResponseEntity.ok(allLines.stream()
                .skip(Math.max(0, size - 100))
                .collect(Collectors.toList()));
    }
}
