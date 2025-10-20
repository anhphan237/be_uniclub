package com.example.uniclub.mapper;

import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Mapper: chuyển đổi ClubApplication → ClubApplicationResponse
 * Dùng MapStruct để tự động map các field cùng tên
 */
@Mapper(componentModel = "spring")
public interface ClubApplicationMapper {

    ClubApplicationMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(ClubApplicationMapper.class);

    // Map entity → DTO (bỏ sourceType)
    @Mapping(source = "proposer", target = "proposer", qualifiedByName = "userToSimpleUser")
    @Mapping(source = "reviewedBy", target = "reviewedBy", qualifiedByName = "userToSimpleUser")
    @Mapping(source = "createdAt", target = "submittedAt")
    @Mapping(source = "reviewedAt", target = "reviewedAt")
    ClubApplicationResponse toResponse(ClubApplication app);

    // === Helper: chuyển User → SimpleUser DTO ===
    @Named("userToSimpleUser")
    default ClubApplicationResponse.SimpleUser userToSimpleUser(User user) {
        if (user == null) return null;
        return ClubApplicationResponse.SimpleUser.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
