package com.shinhan.esg_be.domain.activity.repository;

import com.shinhan.esg_be.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
}
