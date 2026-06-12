package com.enterprise.disaster_recovery.repository;

import com.enterprise.disaster_recovery.entity.BackupMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupMetadataRepository extends JpaRepository<BackupMetadata, String> {
}