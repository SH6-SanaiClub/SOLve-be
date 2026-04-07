package com.shinhan.esg_be.domain.user.repository;

import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
