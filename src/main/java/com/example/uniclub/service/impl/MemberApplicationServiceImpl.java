package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MemberApplication;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ApplicationStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MemberApplicationRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.MemberApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberApplicationServiceImpl implements MemberApplicationService {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final MemberApplicationRepository memberApplicationRepository;

    @Override
    public MemberApplication createApplication(String email, Long clubId) {
        // Lấy user từ email trong token
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Lấy club từ DB
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        // Tạo application
        MemberApplication app = MemberApplication.builder()
                .user(user)
                .club(club)
                .status(ApplicationStatusEnum.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        return memberApplicationRepository.save(app);
    }
}

