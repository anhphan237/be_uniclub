package com.example.uniclub.repository;

import com.example.uniclub.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    boolean existsByName(String name);
    boolean existsByMajorCode(String majorCode);
    Optional<Major> findByMajorCode(String majorCode);

}
