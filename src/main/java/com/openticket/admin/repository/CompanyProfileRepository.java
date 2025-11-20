package com.openticket.admin.repository;

import com.openticket.admin.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<UserProfile, Long> {
}