package com.example.uniclub.service;

import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.request.ClubRenameRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.entity.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClubService {

    // 🟢 1. Tạo CLB thủ công (Admin / Staff)
    ClubResponse create(ClubCreateRequest req);

    // 🟣 2. Lấy thông tin CLB
    ClubResponse get(Long id);

    // 🟦 3. Danh sách CLB (phân trang)
    Page<ClubResponse> list(Pageable pageable);

    // 🔴 4. Xóa CLB
    void delete(Long id);

    // 🟡 5. Lưu CLB (dành cho nội bộ)
    Club saveClub(Club club);

    // 🟩 6. Cập nhật lại số lượng thành viên
    void updateMemberCount(Long clubId);

    Page<ClubResponse> getAvailableForApply(Long userId, String keyword, Pageable pageable);
    ClubResponse renameClub(Long clubId, ClubRenameRequest req, Long requesterId);


}
