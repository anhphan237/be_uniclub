package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.MemberRewardSuggestionResponse;
import com.example.uniclub.service.RewardSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reward/suggestions")
@RequiredArgsConstructor
@Tag(name = "Reward Suggestions", description = "Gợi ý hệ thống về điểm thưởng cho từng thành viên")
public class RewardSuggestionController {

    private final RewardSuggestionService suggestionService;

    @GetMapping("/clubs/{clubId}")
    @Operation(
            summary = "Lấy danh sách gợi ý điểm thưởng cho CLB trong 1 tháng",
            description = "Nếu không truyền year/month thì mặc định lấy tháng hiện tại"
    )
    public ResponseEntity<ApiResponse<List<MemberRewardSuggestionResponse>>> getSuggestions(
            @PathVariable Long clubId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();

        return ResponseEntity.ok(
                ApiResponse.ok(suggestionService.getRewardSuggestions(clubId, y, m))
        );
    }
}
