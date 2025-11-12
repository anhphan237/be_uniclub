package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.ClubActivityStatusEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClubActivityScheduler {

    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final MultiplierPolicyRepository policyRepo;

    /**
     * 🕒 Chạy vào ngày đầu tiên mỗi tháng
     * Cập nhật trạng thái hoạt động và multiplier của CLB
     */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void updateClubActivityStatus() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate start = lastMonth.atDay(1);
        LocalDate end = lastMonth.atEndOfMonth();

        // 🔹 Lấy danh sách chính sách multiplier của CLUB (sắp xếp theo minEventsForClub giảm dần)
        List<MultiplierPolicy> clubPolicies =
                policyRepo.findByTargetTypeOrderByMinEventsForClubDesc(PolicyTargetTypeEnum.CLUB);

        List<Club> clubs = clubRepo.findAll();

        for (Club club : clubs) {
            // 🔹 Đếm số sự kiện CLB tổ chức trong tháng trước
            long eventCount = eventRepo.findByHostClub_ClubId(club.getClubId()).stream()
                    .filter(e -> e.getDate() != null &&
                            !e.getDate().isBefore(start) &&
                            !e.getDate().isAfter(end))
                    .count();

            // 🔹 Tìm chính sách phù hợp nhất
            MultiplierPolicy matchedPolicy = clubPolicies.stream()
                    .filter(p -> eventCount >= (p.getMinEventsForClub() != null ? p.getMinEventsForClub() : 0)
                            && p.isActive())
                    .findFirst()
                    .orElse(null);

            if (matchedPolicy != null) {
                try {
                    // ⚙️ Gán trạng thái hoạt động tương ứng
                    club.setActivityStatus(
                            ClubActivityStatusEnum.valueOf(matchedPolicy.getLevelOrStatus())
                    );
                } catch (IllegalArgumentException ex) {
                    // Nếu DB chứa giá trị không khớp enum
                    club.setActivityStatus(ClubActivityStatusEnum.INACTIVE);
                }
                club.setClubMultiplier(matchedPolicy.getMultiplier());
            } else {
                // ❌ Nếu không có policy nào phù hợp → INACTIVE
                club.setActivityStatus(ClubActivityStatusEnum.INACTIVE);
                club.setClubMultiplier(1.0);
            }
        }

        clubRepo.saveAll(clubs);
        System.out.println("Updated club activity & multiplier for " + lastMonth);
    }
}
