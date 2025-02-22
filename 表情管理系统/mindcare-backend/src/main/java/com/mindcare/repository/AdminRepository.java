package com.mindcare.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mindcare.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    boolean existsByUsername(String username);
} 