package com.example.uniclub.service.impl;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ApplicationStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubApplicationRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.ClubApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final UserRepository userRepository;
    private final ClubApplicationRepository clubApplicationRepository;

    @Override
    public ClubApplication createApplication(String email, String clubName, String description) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        boolean existsPending = clubApplicationRepository
                .findBySubmittedBy_EmailAndStatus(email, ApplicationStatusEnum.PENDING)
                .isPresent();

        if (existsPending) {
            throw new ApiException(HttpStatus.CONFLICT, "Bạn đã có đơn thành lập CLB ở trạng thái PENDING, vui lòng chờ duyệt.");
        }

        ClubApplication app = ClubApplication.builder()
                .clubName(clubName)
                .description(description)
                .submittedBy(user)
                .status(com.example.uniclub.enums.ApplicationStatusEnum.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        return clubApplicationRepository.save(app);
    }

    @Override
    public List<ClubApplication> getAllApplications() {
        return clubApplicationRepository.findAll();
    }

    @Override
    public List<ClubApplication> getApplicationsByStatus(ApplicationStatusEnum status) {
        return clubApplicationRepository.findByStatus(status);
    }
}

