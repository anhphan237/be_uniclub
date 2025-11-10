package com.example.uniclub.service;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;

public interface NotificationService {

    // Khi student apply vào club
    void sendApplicationSubmitted(String studentEmail, String clubName);

    // Khi club leader duyệt hoặc từ chối
    void sendApplicationResult(String studentEmail, String clubName, boolean accepted);

    // Khi club gửi request tạo event -> staff cần duyệt
    void sendEventApprovalRequest(String staffEmail, String clubName, String eventName);

    // Khi staff duyệt hoặc từ chối event
    void sendEventApprovalResult(String leaderEmail, String eventName, boolean approved);
    // 🆕 Thêm các hàm cho flow đa CLB:
    void notifyCoHostInvite(Club coHost, Event event);

    void notifyUniStaffWaiting(Event event);

    void notifyHostEventRejectedByCoHost(Event event, Club coClub);

    void notifyUniStaffReadyForReview(Event event);

    void notifyEventRejected(Event event, User creator);

    void notifyEventApproved(Event event);
    void notifyEventCompleted(Event event);
    void notifyEventPublicCheckin(Event event, User user);

}
