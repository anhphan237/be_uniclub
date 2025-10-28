package com.example.uniclub.repository;

import com.example.uniclub.entity.PointRequest;
import com.example.uniclub.enums.PointRequestStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointRequestRepository extends JpaRepository<PointRequest, Long> {
    List<PointRequest> findByStatus(PointRequestStatusEnum status);
    List<PointRequest> findByClub_ClubId(Long clubId);
}
