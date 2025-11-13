package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.MemberLevelEnum;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
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

    @Scheduled(cron = "0 5 0 1 * *") // chạy 00:05 đầu tháng
    @Transactional
    public void updateMemberLevels() {

        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);

        // 📌 Lấy policy cho MEMBER tham gia EVENT (theo model mới)
        List<MultiplierPolicy> policies = policyRepo
                .findByTargetTypeAndActivityTypeAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.MEMBER_EVENT_PARTICIPATION
                );

        // sort theo minThreshold giảm dần
        policies.sort((a, b) -> b.getMinThreshold() - a.getMinThreshold());

        List<Membership> list = membershipRepo.findAll();

        for (Membership m : list) {

            // 🔢 Đếm số event user tham gia trong tháng trước
            long attendedEvents = regRepo.countByUser_UserIdAndRegisteredAtAfter(
                    m.getUser().getUserId(),
                    oneMonthAgo
            );

            // 🔍 tìm policy phù hợp theo min/max threshold
            MultiplierPolicy matched = findMatchedPolicy(policies, (int) attendedEvents);

            if (matched != null) {

                // set multiplier
                m.setMemberMultiplier(matched.getMultiplier());

                // map ruleName → MemberLevelEnum
                try {
                    m.setMemberLevel(
                            MemberLevelEnum.valueOf(matched.getRuleName().toUpperCase())
                    );
                } catch (Exception ex) {
                    m.setMemberLevel(MemberLevelEnum.BASIC);
                }

            } else {
                // fallback
                m.setMemberMultiplier(1.0);
                m.setMemberLevel(MemberLevelEnum.BASIC);
            }
        }

        membershipRepo.saveAll(list);
    }

    // 🎯 match theo minThreshold / maxThreshold
    private MultiplierPolicy findMatchedPolicy(List<MultiplierPolicy> list, int value) {
        return list.stream()
                .filter(p ->
                        value >= p.getMinThreshold() &&
                                (p.getMaxThreshold() == null || value < p.getMaxThreshold())
                )
                .findFirst()
                .orElse(null);
    }
}
