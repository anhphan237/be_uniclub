package com.example.uniclub.mapper;

import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.entity.MemberApplication;

public class MemberApplicationMapper {
    public static MemberApplicationResponse toResponse(MemberApplication app) {
        return new MemberApplicationResponse(
                app.getApplicationId(),
                app.getUser() != null ? app.getUser().getUserId() : null,
                app.getUser() != null ? app.getUser().getFullName() : null,
                app.getClub() != null ? app.getClub().getClubId() : null,
                app.getClub() != null ? app.getClub().getName() : null,
                app.getStatus(),
                app.getReason(),
                app.getSubmittedAt(),
                app.getUpdatedAt()
        );
    }
}
