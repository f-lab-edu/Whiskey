package com.whiskey.domain.log.repository;

import com.whiskey.domain.log.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

}