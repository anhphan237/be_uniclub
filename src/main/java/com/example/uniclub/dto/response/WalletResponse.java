package com.example.uniclub.dto.response;

import com.example.uniclub.enums.WalletOwnerTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ DTO: Dùng để trả thông tin ví về cho FE mà không trả entity gốc (tránh lỗi Hibernate proxy)
 * Áp dụng cho cả ví CLB và ví User.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private Long walletId;
    private Integer balancePoints;
    private WalletOwnerTypeEnum ownerType;

    // Thông tin cho ví CLB (nếu có)
    private Long clubId;
    private String clubName;

    // Thông tin cho ví User (nếu có)
    private Long userId;
    private String userFullName;
}
