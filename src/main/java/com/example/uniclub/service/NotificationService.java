package com.example.uniclub.service;

public interface NotificationService {

    // Khi student apply vào club
    void sendApplicationSubmitted(String studentEmail, String clubName);

    // Khi club leader duyệt hoặc từ chối
    void sendApplicationResult(String studentEmail, String clubName, boolean accepted);

    // Khi club gửi request tạo event -> staff cần duyệt
    void sendEventApprovalRequest(String staffEmail, String clubName, String eventName);

    // Khi staff duyệt hoặc từ chối event
    void sendEventApprovalResult(String leaderEmail, String eventName, boolean approved);
}
