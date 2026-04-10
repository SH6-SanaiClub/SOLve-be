package com.shinhan.esg_be.domain.user.repository;

import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  
    Optional<User> findFirstByOrderByUserIdAsc();

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByCiDi(String ciDi);

    // 현재 사용자를 제외하고 같은 ciDi가 있는지 확인
    boolean existsByCiDiAndUserIdNot(String ciDi, Long userId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // 현재 사용자를 제외하고 같은 전화번호가 있는지 확인
    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, Long userId);

    Optional<User> findByCiDi(String ciDi);

    boolean existsByLoginIdAndUserIdNot(String loginId, Long userId);

    boolean existsByEmailAndUserIdNot(String email, Long userId);
}
