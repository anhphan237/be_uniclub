package com.example.uniclub.repository;

import com.example.uniclub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByStudentCode(String studentCode); // ✅ Kiểm tra trùng MSSV
}
