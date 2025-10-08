package com.example.uniclub.service.impl;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ApplicationStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubApplicationRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.ClubApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubApplicationServiceImpl implements ClubApplicationService {

    private final UserRepository userRepository;
    private final ClubApplicationRepository clubApplicationRepository;
    private final RabbitTemplate rabbitTemplate;

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

    @Override
    public ClubApplication updateStatus(Long id, ApplicationStatusEnum status, String reviewerEmail) {
        ClubApplication app = clubApplicationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reviewer not found"));

        app.setStatus(status);
        app.setReviewedBy(reviewer);
        app.setReviewedAt(LocalDateTime.now());

        ClubApplication savedApp = clubApplicationRepository.save(app);

//        Map<String, Object> message = new HashMap<>();
//        message.put("applicationId", savedApp.getApplicationId());
//        message.put("clubName", savedApp.getClubName());
//        message.put("status", savedApp.getStatus().name());
//        message.put("reviewedBy", reviewer.getEmail());
//
//        rabbitTemplate.convertAndSend(
//                RabbitMQConfig.CLUB_APP_EXCHANGE,
//                RabbitMQConfig.CLUB_APP_ROUTING_KEY,
//                message
//        );

        return savedApp;
    }
}

