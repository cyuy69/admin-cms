package com.openticket.admin.repository;

import com.openticket.admin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 如果你要用 email 查詢登入者，可以加這個：
    User findByEmail(String email);
}
