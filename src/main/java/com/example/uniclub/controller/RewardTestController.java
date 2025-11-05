package com.example.uniclub.controller;

import com.example.uniclub.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Reward Email Test",
        description = """
        🧪 **Reward Test API** – Kiểm thử gửi email tự động trong hệ thống UniClub.<br>
        Các API này chỉ dùng nội bộ cho mục đích kiểm thử email từ **RewardService**:<br>
        - Gửi email khi thành viên **check-in sự kiện**.<br>
        - Gửi email **thưởng điểm thủ công**.<br>
        - Gửi email khi đạt **mốc điểm thưởng (milestone)**.<br><br>
        ⚠️ Dành cho môi trường DEV/TEST — không nên sử dụng trong production.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reward-test")
@RequiredArgsConstructor
public class RewardTestController {

    private final RewardService rewardService;

    // ============================================================
    // 🧪 1️⃣ Test gửi email khi CHECK-IN sự kiện
    // ============================================================
    @Operation(
            summary = "Gửi email thưởng khi thành viên check-in sự kiện",
            description = """
                Dùng để **kiểm thử** hệ thống gửi email tự động sau khi sinh viên tham gia sự kiện.<br>
                Gửi thông tin:
                - `userId`: ID người dùng
                - Tên sự kiện (mặc định: *Green Planet Festival*)
                - Điểm thưởng check-in (mặc định: 10)
                - Tổng điểm hiện tại (mặc định: 120)
                """
    )
    @PostMapping("/checkin")
    public String testCheckin(@RequestParam Long userId) {
        rewardService.sendCheckInRewardEmail(userId, "Green Planet Festival", 10, 120);
        return "✅ Check-in reward email sent!";
    }

    // ============================================================
    // 🧪 2️⃣ Test gửi email THƯỞNG ĐIỂM thủ công
    // ============================================================
    @Operation(
            summary = "Gửi email thưởng điểm thủ công cho thành viên",
            description = """
                Dùng để **test chức năng gửi email thưởng điểm** khi University Staff hoặc hệ thống tự động cộng điểm.<br>
                Gửi thông tin:
                - `userId`: ID người dùng
                - Điểm thưởng thêm (mặc định: 20)
                - Lý do (mặc định: *Volunteering in UniFair*)
                - Tổng điểm hiện tại (mặc định: 150)
                """
    )
    @PostMapping("/bonus")
    public String testBonus(@RequestParam Long userId) {
        rewardService.sendManualBonusEmail(userId, 20, "Volunteering in UniFair", 150);
        return "✅ Bonus reward email sent!";
    }

    // ============================================================
    // 🧪 3️⃣ Test gửi email đạt MỐC THƯỞNG
    // ============================================================
    @Operation(
            summary = "Gửi email chúc mừng khi đạt mốc điểm thưởng (milestone)",
            description = """
                Dùng để **kiểm thử tính năng milestone reward email**.<br>
                Khi người dùng đạt mốc điểm cụ thể (ví dụ: 500 điểm), hệ thống gửi email chúc mừng và thống kê tiến trình.
                """
    )
    @PostMapping("/milestone")
    public String testMilestone(@RequestParam Long userId) {
        rewardService.sendMilestoneEmail(userId, 500);
        return "✅ Milestone reward email sent!";
    }
}
