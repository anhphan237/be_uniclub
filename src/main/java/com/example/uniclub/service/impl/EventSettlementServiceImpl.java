package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventRegistration;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.service.EventSettlementService;
import com.example.uniclub.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSettlementServiceImpl implements EventSettlementService {

    private final EventRegistrationRepository registrationRepo;
    private final WalletService walletService;
    private final EventRepository eventRepo;

    @Override
    @Transactional
    public void settleEvent(Event event) {
        Wallet eventWallet = event.getWallet();
        if (eventWallet == null) {
            throw new IllegalStateException("Event wallet not found for settlement.");
        }

        List<EventRegistration> regs = registrationRepo.findByEvent_EventId(event.getEventId());

        for (EventRegistration r : regs) {
            long commit = r.getCommittedPoints() == null ? 0L : r.getCommittedPoints().longValue();
            if (commit <= 0) continue;

            long payout = switch (r.getAttendanceLevel() == null ? AttendanceLevelEnum.NONE : r.getAttendanceLevel()) {
                case FULL -> 2L * commit;  // hoàn + thưởng x2
                case HALF -> commit;       // hoàn x1
                default -> 0L;             // NONE / SUSPICIOUS -> không hoàn
            };

            if (payout > 0) {
                Membership membership = r.getUser().getMemberships().stream()
                        .filter(m -> m.getClub().equals(event.getHostClub()))
                        .findFirst()
                        .orElse(null);

                if (membership != null && walletService.getOrCreateUserWallet(membership.getUser()) != null) {
                    String note = (payout == commit)
                            ? WalletTransactionTypeEnum.REFUND_COMMIT.name()
                            : WalletTransactionTypeEnum.BONUS_REWARD.name();

                    // ✅ Ép kiểu về int để khớp hàm trong WalletService
                    walletService.transferPoints(eventWallet, walletService.getOrCreateUserWallet(membership.getUser()), (int) payout, note);
                }
            }
        }

        // 🔹 Sau khi settle xong → chuyển sang COMPLETED (theo enum mới)
        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);
    }
}
