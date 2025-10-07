package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.enums.ApplicationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubApplicationRepository extends JpaRepository<ClubApplication, Long> {
    List<ClubApplication> findByStatus(ApplicationStatusEnum status);
    Optional<ClubApplication> findBySubmittedBy_EmailAndStatus(String email, ApplicationStatusEnum status);

}
