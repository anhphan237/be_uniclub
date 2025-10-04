package com.example.uniclub.repository;

import com.example.uniclub.entity.RolePermission;
import com.example.uniclub.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {}
