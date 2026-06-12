package com.enterprise.disaster_recovery.controller;

import com.enterprise.disaster_recovery.repository.BackupMetadataRepository;
import com.enterprise.disaster_recovery.service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backup")
public class BackupController {

    private final BackupService backupService;
    private final BackupMetadataRepository metadataRepository;

    public BackupController(BackupService backupService, BackupMetadataRepository metadataRepository) {
        this.backupService = backupService;
        this.metadataRepository = metadataRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startBackup(@RequestParam(defaultValue = "FULL") String type) {
        String backupId;
        if ("FILES_ONLY".equalsIgnoreCase(type)) {
            backupId = backupService.initiateFileBackup();
        } else if ("DB_ONLY".equalsIgnoreCase(type)) {
            backupId = backupService.initiateDatabaseBackup();
        } else {
            backupId = backupService.initiateFullBackup();
        }
        
        return ResponseEntity.accepted().body(Map.of(
                "message", "Backup initiated successfully",
                "backupId", backupId,
                "type", type
        ));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> getBackupStatus(@PathVariable String id) {
        return metadataRepository.findById(id)
                .map(metadata -> ResponseEntity.ok(metadata))
                .orElse(ResponseEntity.notFound().build());
    }
}