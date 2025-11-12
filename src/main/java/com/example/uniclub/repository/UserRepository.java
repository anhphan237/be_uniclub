package com.example.uniclub.repository;

import com.example.uniclub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByStudentCode(String studentCode);

    // 🔍 Tìm kiếm user theo tên / email / MSSV (phục vụ cho /search)
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrStudentCodeContainingIgnoreCase(
            String fullName, String email, String studentCode, Pageable pageable
    );

    // 🎭 Lọc danh sách user theo vai trò (phục vụ cho /role/{roleName})
    Page<User> findByRole_RoleNameIgnoreCase(String roleName, Pageable pageable);

    // 📊 Đếm user theo trạng thái (ACTIVE / INACTIVE)
    long countByStatus(String status);

    // 📊 Thống kê user theo role (phục vụ cho /stats)
    @Query("SELECT u.role.roleName, COUNT(u) FROM User u GROUP BY u.role.roleName")
    List<Object[]> countGroupByRole();

    Page<User> findByFullNameContainingIgnoreCase(String keyword, Pageable pageable);
    // 📊 Thống kê số lượng sinh viên theo ngành
    @Query("""
    SELECT u.major.name AS majorName, COUNT(u) AS studentCount
    FROM User u
    WHERE u.role.roleName = 'STUDENT' AND u.major IS NOT NULL
    GROUP BY u.major.name
    ORDER BY COUNT(u) DESC
""")
    List<Object[]> countStudentsByMajor();

}
