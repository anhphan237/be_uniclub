package com.example.uniclub.repository;

import com.example.uniclub.entity.MajorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MajorPolicyRepository extends JpaRepository<MajorPolicy, Long> {
}
