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

        // 🔹 Lấy danh sách chính sách multiplier của MEMBER
        List<MultiplierPolicy> memberPolicies =
                policyRepo.findByTargetTypeOrderByMinEventsDesc(PolicyTargetTypeEnum.MEMBER);

        List<Membership> members = membershipRepo.findAll();

        for (Membership m : members) {
            long attendedEvents = regRepo.countByUser_UserIdAndRegisteredAtAfter(
                    m.getUser().getUserId(), oneMonthAgo);

            // 🔹 Tìm chính sách phù hợp nhất
            MultiplierPolicy matchedPolicy = memberPolicies.stream()
                    .filter(p -> attendedEvents >= p.getMinEvents() && p.isActive())
                    .findFirst()
                    .orElse(null);

            if (matchedPolicy != null) {
                try {
                    // ⚙️ Dùng levelOrStatus thay cho level
                    m.setMemberLevel(
                            MemberLevelEnum.valueOf(matchedPolicy.getLevelOrStatus())
                    );
                } catch (IllegalArgumentException ex) {
                    // Nếu giá trị không hợp lệ, fallback về BASIC
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
