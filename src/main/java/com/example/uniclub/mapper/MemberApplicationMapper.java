package com.example.uniclub.mapper;

import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.entity.User;

public class MemberApplicationMapper {

    public static MemberApplicationResponse toResponse(MemberApplication app) {
        User user = app.getUser();

        return MemberApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .userId(user.getUserId())
                .userName(user.getFullName())
                .clubId(app.getClub().getClubId())
                .clubName(app.getClub().getName())
                .status(app.getStatus())
                .reason(app.getReason())
                .reviewedBy(app.getReviewedBy() != null ? app.getReviewedBy().getFullName() : null)
                .submittedAt(app.getSubmittedAt())
                .updatedAt(app.getUpdatedAt())

                // ✅ Map thêm thông tin profile
                .studentCode(user.getStudentCode())
                .majorName(user.getMajorName())
                .bio(user.getBio())
                .build();
    }
}
