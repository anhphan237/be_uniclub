package com.example.uniclub.service;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.entity.Card;
import java.util.List;

public interface CardService {

    // 🟢 Tạo hoặc cập nhật Card cho 1 CLB
    ApiResponse<Card> saveOrUpdate(Long clubId, Card request);

    // 🔵 Lấy danh sách Card của 1 CLB (thường chỉ 1)
    List<Card> getByClub(Long clubId);

    // 🟣 Lấy chi tiết 1 Card theo ID
    Card getById(Long id);

    // 🔴 Xóa Card
    ApiResponse<String> delete(Long id);
}
