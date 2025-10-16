package com.example.uniclub.service;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ClubApplicationService {

    // 🟢 1. Sinh viên nộp đơn online (ROLE: STUDENT)
    ClubApplicationResponse createOnline(Long proposerId, ClubApplicationCreateRequest req);

    // 🟩 2. Staff nhập đơn offline đã duyệt (ROLE: ADMIN, UNIVERSITY_STAFF)
    ClubApplicationResponse createOffline(Long staffId, ClubApplicationOfflineRequest req);

    // 🟦 3. Lấy danh sách đơn chờ duyệt (ROLE: ADMIN, UNIVERSITY_STAFF)
    List<ClubApplicationResponse> getPending();

    // 🟠 4. Duyệt hoặc từ chối đơn (ROLE: ADMIN, UNIVERSITY_STAFF)
    ClubApplicationResponse decide(Long id, Long staffId, ClubApplicationDecisionRequest req);

    // 🟣 5. Lấy danh sách đơn của 1 sinh viên (ROLE: STUDENT)
    List<ClubApplicationResponse> getByUser(Long userId);

    // 🔵 6. Xem chi tiết 1 đơn (ROLE: ADMIN, UNIVERSITY_STAFF, STUDENT)
    ClubApplicationResponse getById(Long userId, String roleName, Long id);

    // 🟤 7. Lọc đơn theo trạng thái và loại CLB (ROLE: ADMIN, UNIVERSITY_STAFF)
    List<ClubApplicationResponse> filter(String status, String clubType);

    // ⚪ 8. Cập nhật ghi chú nội bộ (ROLE: ADMIN, UNIVERSITY_STAFF)
    ClubApplicationResponse updateNote(Long id, Long staffId, String note);

    // 🟠 9. Xóa đơn sai (ROLE: ADMIN)
    void delete(Long id);

    // 🟢 10. Upload file minh chứng (ROLE: STUDENT, STAFF, ADMIN)
    String uploadFile(Long id, Long userId, MultipartFile file);

    // 🟣 11. Thống kê tổng số đơn (ROLE: ADMIN, STAFF)
    Map<String, Object> getStatistics();

    // 🔵 12. Tìm kiếm đơn theo tên CLB / người nộp (ROLE: ADMIN, STAFF)
    List<ClubApplicationResponse> search(String keyword);
}
