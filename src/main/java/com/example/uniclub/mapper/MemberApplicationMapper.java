package com.example.uniclub.mapper;

import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.entity.MemberApplication;

public class MemberApplicationMapper {
    public static MemberApplicationResponse toResponse(MemberApplication app) {
        return MemberApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .userId(app.getUser().getUserId())
                .userName(app.getUser().getFullName())
                .clubId(app.getClub().getClubId())
                .clubName(app.getClub().getName())
                .status(app.getStatus())
                .reason(app.getReason())
                .reviewedBy(app.getReviewedBy() != null ? app.getReviewedBy().getFullName() : null)
                .submittedAt(app.getSubmittedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
