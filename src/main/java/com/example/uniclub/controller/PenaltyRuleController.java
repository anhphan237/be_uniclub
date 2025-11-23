package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.PenaltyRuleRequest;
import com.example.uniclub.entity.PenaltyRule;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.PenaltyRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/penalty-rules")
@RequiredArgsConstructor
@Tag(
        name = "Penalty Rule Management (UniStaff Only)",
        description = """
            API dành cho UniStaff để tạo & quản lý danh sách vi phạm chuẩn.<br>
            Các Rule này sẽ được Leader sử dụng khi tạo phiếu phạt thành viên trong CLB.<br><br>
            Rule gồm:<br>
            - Tên vi phạm<br>
            - Mô tả / hướng dẫn áp dụng<br>
            - Mức độ (MINOR / NORMAL / MAJOR / SEVERE)<br>
            - Điểm phạt (âm)<br>
            """
)
public class PenaltyRuleController {

    private final PenaltyRuleRepository ruleRepo;

    // ============================================================================
    // 1) CREATE RULE
    // ============================================================================
    @PostMapping
    @Operation(summary = "Tạo rule vi phạm mới (UniStaff)")
    public ResponseEntity<ApiResponse<?>> createRule(
            @Valid @RequestBody PenaltyRuleRequest body
    ) {
        PenaltyRule rule = PenaltyRule.builder()
                .name(body.name())
                .description(body.description())
                .level(body.level())
                .penaltyPoints(body.penaltyPoints())
                .build();

        ruleRepo.save(rule);
        return ResponseEntity.ok(ApiResponse.ok("Rule created successfully."));
    }

    // ============================================================================
    // 2) LIST RULES
    // ============================================================================
    @GetMapping
    @Operation(summary = "Danh sách tất cả rule vi phạm")
    public ResponseEntity<ApiResponse<?>> listRules() {
        List<PenaltyRule> rules = ruleRepo.findAll();
        return ResponseEntity.ok(ApiResponse.ok(rules));
    }

    // ============================================================================
    // 3) GET RULE BY ID
    // ============================================================================
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin một rule")
    public ResponseEntity<ApiResponse<?>> getRule(@PathVariable Long id) {
        PenaltyRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rule not found"));
        return ResponseEntity.ok(ApiResponse.ok(rule));
    }

    // ============================================================================
    // 4) UPDATE RULE
    // ============================================================================
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật rule vi phạm")
    public ResponseEntity<ApiResponse<?>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody PenaltyRuleRequest body
    ) {
        PenaltyRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rule not found"));

        rule.setName(body.name());
        rule.setDescription(body.description());
        rule.setLevel(body.level());
        rule.setPenaltyPoints(body.penaltyPoints());

        ruleRepo.save(rule);
        return ResponseEntity.ok(ApiResponse.ok("Rule updated successfully."));
    }

    // ============================================================================
    // 5) DELETE RULE
    // ============================================================================
    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá rule vi phạm (UniStaff Only)")
    public ResponseEntity<ApiResponse<?>> deleteRule(@PathVariable Long id) {
        if (!ruleRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Rule not found");
        }
        ruleRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Rule deleted successfully."));
    }
}
