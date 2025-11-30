package com.example.uniclub.service;

import com.example.uniclub.entity.OrderActionLog;
import com.example.uniclub.repository.OrderActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderActionLogService {

    private final OrderActionLogRepository logRepo;

    // 🔹 Get all logs
    public List<OrderActionLog> getAllLogs() {
        return logRepo.findAllByOrderByCreatedAtDesc();
    }

    // 🔹 Get logs by targetUser (người đổi / sở hữu order)
    public List<OrderActionLog> getLogsByTargetUser(Long userId) {
        return logRepo.findByTargetUser_UserIdOrderByCreatedAtDesc(userId);
    }

    // 🔹 Get logs by actor (staff/leader thực hiện hành động)
    public List<OrderActionLog> getLogsByActor(Long actorId) {
        return logRepo.findByActor_UserIdOrderByCreatedAtDesc(actorId);
    }
}

