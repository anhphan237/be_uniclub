package com.example.uniclub.mapper;

import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ClubApplicationMapper {

    ClubApplicationMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(ClubApplicationMapper.class);

    @Mapping(source = "submittedBy", target = "submittedBy", qualifiedByName = "userToSimpleUser")
    @Mapping(source = "reviewedBy", target = "reviewedBy", qualifiedByName = "userToSimpleUser")
    @Mapping(source = "status", target = "status")
    ClubApplicationResponse toResponse(ClubApplication app);

    @Named("userToSimpleUser")
    default ClubApplicationResponse.SimpleUser userToSimpleUser(User user) {
        if (user == null) return null;
        return ClubApplicationResponse.SimpleUser.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}


