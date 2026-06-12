package com.enterprise.disaster_recovery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BackupScheduler.class);
    private final BackupService backupService;

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleDailyDatabaseBackup() {
        logger.info("Executing scheduled daily FULL backup...");
        String backupId = backupService.initiateFullBackup();
        logger.info("Scheduled DB backup initiated with ID: {}", backupId);
    }

    // Backup Retention Policy: Runs every Sunday at 3 AM to clean up backups older than 30 days
    @Scheduled(cron = "0 0 3 * * SUN")
    public void scheduleBackupRetentionCleanup() {
        logger.info("Executing backup retention cleanup...");
        backupService.cleanupOldBackups(30);
    }
}