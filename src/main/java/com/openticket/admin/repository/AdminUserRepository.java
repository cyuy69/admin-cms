package com.openticket.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openticket.admin.entity.User;

public interface AdminUserRepository extends JpaRepository<User, Long> {

    @Query("""
                SELECT u FROM User u
                WHERE (:keyword IS NULL
                    OR u.username LIKE CONCAT('%', :keyword, '%')
                    OR u.account LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            Pageable pageable);
}
