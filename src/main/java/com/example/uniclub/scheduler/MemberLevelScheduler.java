package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.MemberLevelEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberLevelScheduler {

    private final MembershipRepository membershipRepo;
    private final EventRegistrationRepository regRepo;
    private final MultiplierPolicyRepository policyRepo;

    /**
     * 🕒 Chạy đầu mỗi tháng để cập nhật cấp độ và multiplier cho thành viên
     */
    @Scheduled(cron = "0 30 0 1 * *") // 00:30 ngày đầu tháng
    @Transactional
    public void updateMemberLevels() {
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);

        // 🔹 Lấy danh sách chính sách multiplier dành cho MEMBER
        List<MultiplierPolicy> memberPolicies =
                policyRepo.findByTargetTypeOrderByMinEventsForClubDesc(PolicyTargetTypeEnum.MEMBER);

        // 🔹 Lấy toàn bộ membership hiện có
        List<Membership> members = membershipRepo.findAll();

        for (Membership m : members) {
            // 🧮 Đếm số sự kiện đã tham gia trong 1 tháng gần nhất
            long attendedEvents = regRepo.countByUser_UserIdAndRegisteredAtAfter(
                    m.getUser().getUserId(), oneMonthAgo);

            // 🔍 Tìm chính sách phù hợp nhất
            MultiplierPolicy matchedPolicy = memberPolicies.stream()
                    .filter(p -> attendedEvents >= (p.getMinEventsForClub() != null ? p.getMinEventsForClub() : 0)
                            && p.isActive())
                    .findFirst()
                    .orElse(null);

            // ⚙️ Cập nhật cấp độ & multiplier
            if (matchedPolicy != null) {
                try {
                    // Dùng levelOrStatus để map sang MemberLevelEnum
                    m.setMemberLevel(
                            MemberLevelEnum.valueOf(matchedPolicy.getLevelOrStatus())
                    );
                } catch (IllegalArgumentException ex) {
                    // Nếu không khớp Enum, fallback BASIC
                    m.setMemberLevel(MemberLevelEnum.BASIC);
                }
                m.setMemberMultiplier(matchedPolicy.getMultiplier());
            } else {
                m.setMemberLevel(MemberLevelEnum.BASIC);
                m.setMemberMultiplier(1.0);
            }
        }

        membershipRepo.saveAll(members);
        System.out.println("Updated member levels & multipliers successfully!");
    }
}
