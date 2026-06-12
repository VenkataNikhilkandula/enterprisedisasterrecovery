package com.enterprise.disaster_recovery.repository;

import com.enterprise.disaster_recovery.entity.RestoreLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestoreLogRepository extends JpaRepository<RestoreLog, String> {
}