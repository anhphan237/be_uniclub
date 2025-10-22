package com.example.uniclub.service;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import java.util.List;
import java.util.Map;

public interface ClubApplicationService {

    // 🟢 1. Sinh viên nộp đơn online (ROLE: STUDENT)
    ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req);

    // 🟦 2. Lấy danh sách đơn chờ duyệt (ROLE: ADMIN, UNIVERSITY_STAFF)
    List<ClubApplicationResponse> getPending();

    // 🟠 3. Duyệt hoặc từ chối đơn (ROLE: ADMIN, UNIVERSITY_STAFF)
    ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req);

    // 🟣 4. Lấy danh sách đơn của 1 sinh viên (ROLE: STUDENT)
    List<ClubApplicationResponse> getByUser(Long userId);

    // 🔵 5. Xem chi tiết 1 đơn (ROLE: ADMIN, UNIVERSITY_STAFF, STUDENT)
    ClubApplicationResponse getById(Long userId, String roleName, Long id);

    // 🟠 6. Xóa đơn sai (ROLE: ADMIN)
    void delete(Long id);

    // 🟣 7. Thống kê tổng số đơn (ROLE: ADMIN, STAFF)
    Map<String, Object> getStatistics();

    // 🔵 8. Tìm kiếm đơn theo tên CLB / người nộp (ROLE: ADMIN, STAFF)
    List<ClubApplicationResponse> search(String keyword);


    // 🟩 10. Lấy toàn bộ ClubApplications (ROLE: ADMIN, STAFF)
    List<ClubApplicationResponse> getAllApplications();
}
