package com.enterprise.disaster_recovery.service;

import com.enterprise.disaster_recovery.entity.BackupMetadata;
import com.enterprise.disaster_recovery.entity.BackupStatus;
import com.enterprise.disaster_recovery.entity.BackupType;
import com.enterprise.disaster_recovery.entity.RestoreLog;
import com.enterprise.disaster_recovery.repository.BackupMetadataRepository;
import com.enterprise.disaster_recovery.repository.RestoreLogRepository;
import com.enterprise.disaster_recovery.util.FileBackupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(RestoreService.class);

    private final BackupMetadataRepository backupRepository;
    private final RestoreLogRepository restoreRepository;
    private final AlertService alertService;

    @Value("${app.primary.storage.path}")
    private String primaryStoragePath;

    @Value("${app.backup.mysql.restore.cmd:mysql}")
    private String mysqlRestoreCmd;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public RestoreService(BackupMetadataRepository backupRepository, RestoreLogRepository restoreRepository, AlertService alertService) {
        this.backupRepository = backupRepository;
        this.restoreRepository = restoreRepository;
        this.alertService = alertService;
    }

    public String initiateRestore(String backupId) {
        Optional<BackupMetadata> backupOpt = backupRepository.findById(backupId);
        if (backupOpt.isEmpty()) {
            throw new IllegalArgumentException("Backup ID not found.");
        }

        BackupMetadata backup = backupOpt.get();
        if (backup.getStatus() != BackupStatus.COMPLETED) {
            throw new IllegalStateException("Cannot restore from a backup that is not COMPLETED.");
        }

        String restoreId = UUID.randomUUID().toString();
        RestoreLog restoreLog = new RestoreLog();
        restoreLog.setId(restoreId);
        restoreLog.setBackupMetadata(backup);
        restoreLog.setStatus(BackupStatus.IN_PROGRESS);
        restoreLog.setTime(LocalDateTime.now());
        restoreLog.setDetails("Restore initiated.");
        restoreRepository.save(restoreLog);

        new Thread(() -> performRestore(restoreLog, backup)).start();
        return restoreId;
    }

    private void performRestore(RestoreLog log, BackupMetadata backup) {
        try {
            if (backup.getType() == BackupType.DB_ONLY || backup.getType() == BackupType.FULL) {
                restoreDatabase(backup);
            }
            if (backup.getType() == BackupType.FILES_ONLY || backup.getType() == BackupType.FULL) {
                restoreFiles(backup);
            }
            
            log.setStatus(BackupStatus.COMPLETED);
            log.setDetails("Restore completed successfully.");
            logger.info("Restore {} for Backup {} completed successfully.", log.getId(), backup.getId());
        } catch (Exception e) {
            log.setStatus(BackupStatus.FAILED);
            log.setDetails("Restore failed: " + e.getMessage());
            alertService.sendAlert("Restore Critical Failure", "Restore ID " + log.getId() + " failed: " + e.getMessage());
            logger.error("Restore {} failed", log.getId(), e);
        } finally {
            restoreRepository.save(log);
        }
    }

    private void restoreDatabase(BackupMetadata backup) throws Exception {
        logger.info("Restoring database from {}", backup.getLocation());
        ProcessBuilder processBuilder = new ProcessBuilder(
                "cmd.exe", "/c",
                mysqlRestoreCmd + " -u" + dbUser + " -p" + dbPassword + " disaster_recovery_db < " + backup.getLocation()
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Database restore process exited with code " + exitCode);
        }
    }

    private void restoreFiles(BackupMetadata backup) throws Exception {
        logger.info("Restoring files from {}", backup.getLocation());
        Path sourcePath = Paths.get(backup.getLocation());
        
        File primaryDir = new File(primaryStoragePath);
        if (!primaryDir.exists()) primaryDir.mkdirs();
        
        Path destPath = Paths.get(primaryStoragePath, "restored_archive.tar.gz");
        boolean isConsistent = FileBackupUtil.copyFileWithIntegrityCheck(sourcePath, destPath);
        if (!isConsistent) {
            throw new RuntimeException("File restore integrity check failed. Checksums do not match.");
        }
    }
}