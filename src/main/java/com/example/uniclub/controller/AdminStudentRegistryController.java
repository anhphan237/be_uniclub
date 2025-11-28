package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.service.StudentRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/university/student-registry")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminStudentRegistryController {

    private final StudentRegistryService studentRegistryService;

    // ======================================================================
    // UPLOAD CSV / EXCEL
    // ======================================================================
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Upload danh sách MSSV (CSV hoặc Excel)",
            description = """
                API dùng để import danh sách sinh viên thật vào hệ thống.
                
                ✅ Hỗ trợ file: CSV, XLSX
                -- CSV format: 2 cột -> student_code, full_name
                -- Excel format: 2 cột -> student_code, full_name

                🧠 Hệ thống tự động phân tích student_code:
                - majorCode (2 ký tự đầu)
                - intake (2 ký tự tiếp theo, ví dụ 17 → khóa 2017)
                - orderNumber (4 ký tự cuối)

                ⚠ Các dòng sau sẽ bị bỏ qua tự động:
                - Sai format MSSV (không đúng XXYYZZZZ)
                - Mã ngành không tồn tại
                - MSSV đã tồn tại trong registry
                - Dòng trống hoặc không đủ cột

                👍 Trả về:
                - imported: số lượng dòng được import
                - skipped: số dòng bị bỏ qua
                - total: tổng số dòng
                """
    )
    public ResponseEntity<ApiResponse<Object>> uploadCsv(
            @Parameter(
                    description = "File CSV/XLSX chứa danh sách MSSV để import",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(studentRegistryService.importCsv(file)));
    }

    // ======================================================================
    // GET ALL
    // ======================================================================
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Lấy toàn bộ danh sách sinh viên trong registry",
            description = """
                API trả về toàn bộ danh sách sinh viên thật đã được import.
                
                Dùng trong:
                - Kiểm tra dữ liệu import
                - Debug khi hệ thống báo lỗi MSSV
                - Dùng UI admin để hiển thị danh sách sinh viên
                """
    )
    public ResponseEntity<ApiResponse<Object>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(studentRegistryService.getAll()));
    }

    // ======================================================================
    // CHECK STUDENT EXISTS
    // ======================================================================
    @GetMapping("/check/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Kiểm tra MSSV có tồn tại hay không",
            description = """
                Dùng để kiểm tra tính hợp lệ của một MSSV.
                
                ⚙ Hệ thống sẽ:
                - Chuẩn hóa mã (trim, uppercase)
                - Kiểm tra đúng định dạng XXYYZZZZ
                - Kiểm tra majorCode có tồn tại
                - Kiểm tra MSSV có nằm trong registry
                
                Trả về thông tin đầy đủ của sinh viên nếu hợp lệ.
                """
    )
    public ResponseEntity<ApiResponse<Object>> checkStudent(@PathVariable String code) {
        return ResponseEntity.ok(
                ApiResponse.ok(studentRegistryService.validate(code))
        );
    }

    // ======================================================================
    // SEARCH BY KEYWORD
    // ======================================================================
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Tìm kiếm sinh viên theo tên hoặc MSSV",
            description = """
                Tìm kiếm theo keyword, hỗ trợ:
                - Tìm theo MSSV (ví dụ: SE170)
                - Tìm theo tên (ví dụ: 'Nguyen', 'Trí', 'Minh', ...)
                
                Dùng để:
                - Hỗ trợ staff tìm thông tin nhanh
                - UI lọc dữ liệu
                - Xử lý các tình huống user nhập sai tên
                
                ⚠ Tìm kiếm dạng LIKE, không phân biệt hoa/thường.
                """
    )
    public ResponseEntity<ApiResponse<Object>> search(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(studentRegistryService.search(keyword))
        );
    }

    // ======================================================================
    // DELETE BY CODE
    // ======================================================================
    @DeleteMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Xóa một sinh viên khỏi registry",
            description = """
                Xóa thủ công một sinh viên khỏi registry.
                
                Dùng trong:
                - Chỉnh sửa khi nhập nhầm
                - Sinh viên nghỉ học / không còn hợp lệ
                - Dọn dẹp dữ liệu
                
                ⚠ Chỉ xóa trong student_registry, không ảnh hưởng user table.
                """
    )
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String code) {
        studentRegistryService.delete(code);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // ======================================================================
    // DELETE ALL
    // ======================================================================
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xóa toàn bộ danh sách registry (ADMIN only)",
            description = """
                Xóa sạch toàn bộ student_registry. 
                
                Dùng khi:
                - Import dữ liệu hoàn toàn mới theo năm học
                - Dọn dẹp data lỗi hoặc sai
                
                ⚠ Chỉ ADMIN mới được phép dùng API này.
                ⚠ Cần cân nhắc vì hành động KHÔNG THỂ UNDO.
                """
    )
    public ResponseEntity<ApiResponse<Object>> deleteAll() {
        studentRegistryService.clearAll();
        return ResponseEntity.ok(ApiResponse.msg("All registry entries removed"));
    }
}
