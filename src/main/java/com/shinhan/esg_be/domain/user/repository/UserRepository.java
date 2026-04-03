package com.shinhan.esg_be.domain.user.repository;

import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인 아이디로 유저 찾기
    Optional<User> findByLoginId(String loginId);

    // 아이디 중복 확인용
    boolean existsByLoginId(String loginId);

    // 1인 1계정(CI/DI) 중복 확인용
    boolean existsByCiDi(String ciDi);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
