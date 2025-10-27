package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.repository.*;
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
        List<EventRegistration> regs = registrationRepo.findByEvent_EventId(event.getEventId());

        for (EventRegistration r : regs) {
            int commit = r.getCommittedPoints();
            int payout = switch (r.getAttendanceLevel()) {
                case FULL -> 2 * commit;
                case HALF -> commit;
                default -> 0;
            };

            if (payout > 0) {
                Membership membership = r.getUser().getMemberships().stream()
                        .filter(m -> m.getClub().equals(event.getHostClub()))
                        .findFirst().orElse(null);
                if (membership != null) {
                    walletService.transferPoints(eventWallet, membership.getWallet(), payout,
                            payout == commit ? WalletTransactionTypeEnum.REFUND_COMMIT.name()
                                    : WalletTransactionTypeEnum.BONUS_REWARD.name());
                }
            }
        }

        event.setStatus(EventStatusEnum.SETTLED);
        eventRepo.save(event);
    }
}
