package com.example.uniclub.mapper;

import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.entity.User;

public class MemberApplicationMapper {

    public static MemberApplicationResponse toResponse(MemberApplication app) {
        User user = app.getApplicant();

        return MemberApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .clubId(app.getClub().getClubId())
                .clubName(app.getClub().getName())
                .applicantId(user.getUserId())
                .applicantName(user.getFullName())
                .applicantEmail(user.getEmail())
                .status(app.getStatus().name()) // Enum to String
                .message(app.getMessage())
                .reason(app.getNote())
                .handledById(app.getHandledBy() != null ? app.getHandledBy().getUserId() : null)
                .handledByName(app.getHandledBy() != null ? app.getHandledBy().getFullName() : null)
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())

                // ✅ thêm thông tin user
                .studentCode(user.getStudentCode())
                .build();
    }
}
