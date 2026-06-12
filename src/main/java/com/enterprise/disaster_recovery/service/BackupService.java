package com.enterprise.disaster_recovery.service;

import com.enterprise.disaster_recovery.entity.BackupMetadata;
import com.enterprise.disaster_recovery.entity.BackupStatus;
import com.enterprise.disaster_recovery.entity.BackupType;
import com.enterprise.disaster_recovery.repository.BackupMetadataRepository;
import com.enterprise.disaster_recovery.util.CryptoUtil;
import com.enterprise.disaster_recovery.util.FileBackupUtil;
import com.enterprise.disaster_recovery.util.ZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private final BackupMetadataRepository metadataRepository;
    private final AlertService alertService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Value("${app.primary.storage.path}")
    private String primaryStoragePath;

    @Value("${app.backup.storage.path}")
    private String backupStoragePath;

    @Value("${app.backup.mysql.dump.cmd:mysqldump}")
    private String mysqlDumpCmd;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public BackupService(BackupMetadataRepository metadataRepository, AlertService alertService) {
        this.metadataRepository = metadataRepository;
        this.alertService = alertService;
    }

    public String initiateDatabaseBackup() {
        String backupId = UUID.randomUUID().toString();
        BackupMetadata metadata = new BackupMetadata();
        metadata.setId(backupId);
        metadata.setType(BackupType.DB_ONLY);
        metadata.setTimestamp(LocalDateTime.now());
        metadata.setStatus(BackupStatus.IN_PROGRESS);
        metadataRepository.save(metadata);

        executorService.submit(() -> performDatabaseBackup(metadata));
        return backupId;
    }

    public String initiateFileBackup() {
        String backupId = UUID.randomUUID().toString();
        BackupMetadata metadata = new BackupMetadata();
        metadata.setId(backupId);
        metadata.setType(BackupType.FILES_ONLY);
        metadata.setTimestamp(LocalDateTime.now());
        metadata.setStatus(BackupStatus.IN_PROGRESS);
        metadataRepository.save(metadata);

        executorService.submit(() -> performFileBackup(metadata));
        return backupId;
    }

    public String initiateFullBackup() {
        String backupId = UUID.randomUUID().toString();
        BackupMetadata metadata = new BackupMetadata();
        metadata.setId(backupId);
        metadata.setType(BackupType.FULL);
        metadata.setTimestamp(LocalDateTime.now());
        metadata.setStatus(BackupStatus.IN_PROGRESS);
        metadataRepository.save(metadata);

        executorService.submit(() -> {
            try {
                performDatabaseBackup(metadata);
                performFileBackup(metadata);
            } catch (Exception e) {
                logger.error("FULL backup failed", e);
            }
        });
        return backupId;
    }

    private void performDatabaseBackup(BackupMetadata metadata) {
        try {
            File backupDir = new File(backupStoragePath);
            if (!backupDir.exists()) backupDir.mkdirs();

            String filename = "db_backup_" + metadata.getId() + ".sql";
            String fullPath = backupStoragePath + filename;

            ProcessBuilder processBuilder = new ProcessBuilder(
                    mysqlDumpCmd,
                    "-u" + dbUser,
                    "-p" + dbPassword,
                    "disaster_recovery_db"
            );
            processBuilder.redirectOutput(new File(fullPath));
            processBuilder.redirectErrorStream(true);

            int maxRetries = 3;
            int attempt = 0;
            boolean success = false;
            while (attempt < maxRetries && !success) {
                try {
                    attempt++;
                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        File sqlFile = new File(fullPath);
                        File zipped = ZipUtil.compressFile(sqlFile);
                        File encrypted = CryptoUtil.encryptFile(zipped);
                        
                        Path finalPath = encrypted.toPath();
                        metadata.setLocation(finalPath.toString());
                        metadata.setStatus(BackupStatus.COMPLETED);
                        metadata.setSizeBytes(Files.size(finalPath));
                        logger.info("Database backup {} completed successfully.", metadata.getId());
                        success = true;
                    } else {
                        if (attempt == maxRetries) {
                            metadata.setStatus(BackupStatus.FAILED);
                            alertService.sendAlert("DB Backup Failed", "Backup ID " + metadata.getId() + " failed after 3 attempts.");
                        }
                    }
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        metadata.setStatus(BackupStatus.FAILED);
                        alertService.sendAlert("DB Backup Exception", e.getMessage());
                    }
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            metadata.setStatus(BackupStatus.FAILED);
            alertService.sendAlert("DB Backup Critical Failure", e.getMessage());
        } finally {
            metadataRepository.save(metadata);
        }
    }

    private void performFileBackup(BackupMetadata metadata) {
        try {
            File primaryDir = new File(primaryStoragePath);
            File backupDir = new File(backupStoragePath);
            if (!primaryDir.exists()) primaryDir.mkdirs();
            if (!backupDir.exists()) backupDir.mkdirs();

            Path primaryArchive = Paths.get(primaryStoragePath, "storage_archive.tar.gz");
            if (!Files.exists(primaryArchive)) {
                byte[] dummyData = new byte[1024 * 1024 * 1]; // 1 MB dummy file
                Files.write(primaryArchive, dummyData);
            }

            String filename = "file_backup_" + metadata.getId() + ".tar.gz";
            Path backupArchive = Paths.get(backupStoragePath, filename);

            boolean integrityMaintained = FileBackupUtil.copyFileWithIntegrityCheck(primaryArchive, backupArchive);

            if (integrityMaintained) {
                File zipped = ZipUtil.compressFile(backupArchive.toFile());
                File encrypted = CryptoUtil.encryptFile(zipped);

                metadata.setLocation(encrypted.getAbsolutePath());
                metadata.setChecksum(FileBackupUtil.calculateChecksum(encrypted.toPath()));
                metadata.setSizeBytes(Files.size(encrypted.toPath()));
                metadata.setStatus(BackupStatus.COMPLETED);
                logger.info("File backup {} completed successfully.", metadata.getId());
            } else {
                metadata.setStatus(BackupStatus.FAILED);
            }
        } catch (Exception e) {
            metadata.setStatus(BackupStatus.FAILED);
            logger.error("File backup failed", e);
        } finally {
            metadataRepository.save(metadata);
        }
    }

    public void cleanupOldBackups(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<BackupMetadata> oldBackups = metadataRepository.findAll().stream()
                .filter(b -> b.getTimestamp().isBefore(cutoffDate))
                .toList();
        
        for (BackupMetadata oldBackup : oldBackups) {
            try {
                if (oldBackup.getLocation() != null) {
                    File archive = new File(oldBackup.getLocation());
                    if (archive.exists()) {
                        archive.delete();
                    }
                }
                metadataRepository.delete(oldBackup);
                logger.info("Deleted old backup {} under retention policy.", oldBackup.getId());
            } catch (Exception e) {
                logger.error("Failed to delete old backup {}", oldBackup.getId(), e);
            }
        }
    }
}