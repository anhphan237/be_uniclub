package com.example.uniclub.repository;

import com.example.uniclub.entity.StudentRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRegistryRepository extends JpaRepository<StudentRegistry, Long> {
    Optional<StudentRegistry> findByStudentCode(String studentCode);
}
