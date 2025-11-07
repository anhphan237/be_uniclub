package com.example.uniclub.controller;

import com.example.uniclub.entity.EventLog;
import com.example.uniclub.service.EventLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(
        name = "Event Logs",
        description = " API quản lý và tra cứu lịch sử hoạt động của người dùng trong hệ thống UniClub. " +
                "Bao gồm các hành động như: tham gia CLB, rời CLB, check-in sự kiện, nhận thưởng, đổi sản phẩm, v.v."
)
public class EventLogController {

    private final EventLogService eventLogService;

    // ===============================================================
    // 🔹 Lấy danh sách log theo sự kiện
    // ===============================================================
    @Operation(
            summary = " Lấy danh sách log theo sự kiện",
            description = """
            API này trả về toàn bộ lịch sử hoạt động liên quan đến một sự kiện cụ thể, 
            bao gồm các hành động như: thành viên check-in, check-out, redeem sản phẩm, hoặc tham gia/thoát sự kiện.

             Dữ liệu được sắp xếp theo thời gian mới nhất (createdAt giảm dần).
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Danh sách log được trả về thành công",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EventLog.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy sự kiện hoặc chưa có log nào")
            }
    )
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventLog>> getEventLogs(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventLogService.getLogsByEvent(eventId));
    }

    // ===============================================================
    // 🔹 Lấy danh sách log theo người dùng
    // ===============================================================
    @Operation(
            summary = " Lấy lịch sử log của một người dùng",
            description = """
            API này cho phép truy vấn toàn bộ các hành động mà một người dùng đã thực hiện trong hệ thống UniClub, 
            ví dụ như: tham gia CLB, rời CLB, check-in sự kiện, redeem sản phẩm, hoặc chuyển điểm.

             Dữ liệu được sắp xếp theo thứ tự thời gian mới nhất (createdAt giảm dần).
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Danh sách log của người dùng được trả về thành công",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EventLog.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng hoặc chưa có log nào")
            }
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EventLog>> getUserLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(eventLogService.getLogsByUser(userId));
    }
}
