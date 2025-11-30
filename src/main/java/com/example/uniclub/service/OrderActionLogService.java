package com.example.uniclub.service;

import com.example.uniclub.dto.response.OrderActionLogResponse;
import com.example.uniclub.entity.OrderActionLog;
import com.example.uniclub.repository.OrderActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderActionLogService {

    private final OrderActionLogRepository logRepo;

    public List<OrderActionLogResponse> getAllLogs() {
        return logRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderActionLogResponse> getLogsByTargetUser(Long userId) {
        return logRepo.findByTargetUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderActionLogResponse> getLogsByActor(Long actorId) {
        return logRepo.findByActor_UserIdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderActionLogResponse toResponse(OrderActionLog log) {
        return new OrderActionLogResponse(
                log.getId(),
                log.getAction().name(),

                log.getActor() != null ? log.getActor().getUserId() : null,
                log.getActor() != null ? log.getActor().getFullName() : null,

                log.getTargetUser() != null ? log.getTargetUser().getUserId() : null,
                log.getTargetUser() != null ? log.getTargetUser().getFullName() : null,

                log.getOrder() != null ? log.getOrder().getOrderId() : null,

                log.getQuantity(),
                log.getPointsChange(),
                log.getReason(),
                log.getCreatedAt()
        );
    }

}

