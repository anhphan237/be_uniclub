package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.ClubActivityStatusEnum;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
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

    @Scheduled(cron = "0 0 0 1 * *") // chạy đầu tháng
    @Transactional
    public void updateClubActivityStatus() {

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate start = lastMonth.atDay(1);
        LocalDate end = lastMonth.atEndOfMonth();

        // 📌 Lấy chính sách dành cho CLUB
        List<MultiplierPolicy> policies = policyRepo
                .findByTargetTypeAndActivityTypeAndActiveTrue(
                        PolicyTargetTypeEnum.CLUB,
                        PolicyActivityTypeEnum.CLUB_EVENT_ACTIVITY

                );

        List<Club> clubs = clubRepo.findAll();

        for (Club club : clubs) {

            // Đếm số event CLB đã tổ chức tháng trước
            long count = eventRepo.findByHostClub_ClubId(club.getClubId())
                    .stream()
                    .filter(ev -> ev.getDate() != null &&
                            !ev.getDate().isBefore(start) &&
                            !ev.getDate().isAfter(end))
                    .count();

            // 🔍 Chọn policy tương ứng
            MultiplierPolicy matched = findMatchedPolicy(policies, (int) count);

            // ⚙️ Cập nhật vào CLB
            if (matched != null) {
                club.setClubMultiplier(matched.getMultiplier());

                // Nếu tên rule khớp enum → set activityStatus
                try {
                    club.setActivityStatus(
                            ClubActivityStatusEnum.valueOf(matched.getRuleName().toUpperCase())
                    );
                } catch (Exception ex) {
                    club.setActivityStatus(ClubActivityStatusEnum.INACTIVE);
                }
            } else {
                club.setClubMultiplier(1.0);
                club.setActivityStatus(ClubActivityStatusEnum.INACTIVE);
            }
        }

        clubRepo.saveAll(clubs);
    }

    /** Tìm policy phù hợp theo ngưỡng min/max */
    private MultiplierPolicy findMatchedPolicy(List<MultiplierPolicy> list, int value) {
        return list.stream()
                .filter(p -> value >= p.getMinThreshold() &&
                        (p.getMaxThreshold() == null || value < p.getMaxThreshold()))
                .findFirst()
                .orElse(null);
    }
}
