package com.example.uniclub.service;

import com.example.uniclub.entity.Notification;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.enums.NotificationStatusEnum;
import com.example.uniclub.enums.NotificationTypeEnum;
import com.example.uniclub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletNotificationService {

    private final NotificationRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi thông báo realtime + lưu DB khi có giao dịch ví
     */
    public void sendWalletTransactionNotification(WalletTransaction tx) {
        try {
            if (tx.getWallet() == null || tx.getWallet().getUser() == null) {
                log.warn("⚠️ Skipped wallet notification: no user found in wallet");
                return;
            }

            // 🧾 Tạo message hiển thị
            String symbol = tx.getAmount() > 0 ? "➕" : "➖";
            String message = "%s %s %d points (%s)"
                    .formatted(
                            symbol,
                            tx.getAmount() > 0 ? "Received" : "Spent",
                            Math.abs(tx.getAmount()),
                            tx.getType().name()
                    );

            // 💾 Lưu vào DB
            Notification noti = Notification.builder()
                    .user(tx.getWallet().getUser())
                    .message(message)
                    .type(NotificationTypeEnum.SYSTEM)
                    .status(NotificationStatusEnum.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepo.save(noti);

            // 🔔 Gửi realtime đến FE (qua WebSocket)
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(tx.getWallet().getUser().getUserId()),
                    "/queue/notifications",
                    noti
            );

            log.info("📢 Wallet notification sent to user {}: {}",
                    tx.getWallet().getUser().getFullName(), message);

        } catch (Exception e) {
            log.error("❌ Failed to send wallet transaction notification: {}", e.getMessage());
        }
    }
}
