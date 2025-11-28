package com.example.uniclub.repository;

import com.example.uniclub.entity.StudentRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRegistryRepository extends JpaRepository<StudentRegistry, Long> {
    Optional<StudentRegistry> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);

    @Query("SELECT s FROM StudentRegistry s WHERE s.studentCode LIKE %?1% OR s.fullName LIKE %?2%")
    List<StudentRegistry> searchByCodeOrName(String code, String name);

    void deleteByStudentCode(String studentCode);

}
