package com.shinhan.esg_be.domain.stat.repository;

import com.shinhan.esg_be.domain.stat.entity.UserScoreSnapshot;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserScoreSnapshotRepository extends JpaRepository<UserScoreSnapshot, Long> {

    Optional<UserScoreSnapshot> findByUserAndSnapshotDate(User user, LocalDate snapshotDate);

    List<UserScoreSnapshot> findByUser_UserIdOrderBySnapshotDateDesc(Long userId);
}
