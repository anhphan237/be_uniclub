package com.example.uniclub.controller;



import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.AdminSummaryResponse;
import com.example.uniclub.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.uniclub.service.AdminStatisticService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminStatisticService adminStatisticService;

    @Operation(summary = " Tổng hợp dữ liệu hệ thống cho Admin Dashboard")
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminDashboardService.getSummary());
    }
    @Operation(
            summary = "Thống kê số lượng sinh viên theo ngành",
            description = """
            API này trả về danh sách các ngành học cùng với số lượng sinh viên (role = STUDENT)
            đang hoạt động trong hệ thống, sắp xếp theo thứ tự giảm dần.
            """
    )
    @GetMapping("/students-by-major")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudentCountByMajor() {
        return ResponseEntity.ok(ApiResponse.ok(adminStatisticService.getStudentCountByMajor()));
    }
}
