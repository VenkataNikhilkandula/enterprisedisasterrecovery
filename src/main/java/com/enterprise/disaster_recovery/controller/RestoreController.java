package com.enterprise.disaster_recovery.controller;

import com.enterprise.disaster_recovery.entity.RestoreLog;
import com.enterprise.disaster_recovery.repository.RestoreLogRepository;
import com.enterprise.disaster_recovery.service.RestoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/restore")
public class RestoreController {

    private final RestoreService restoreService;
    private final RestoreLogRepository restoreRepository;

    public RestoreController(RestoreService restoreService, RestoreLogRepository restoreRepository) {
        this.restoreService = restoreService;
        this.restoreRepository = restoreRepository;
    }

    @PostMapping("/{backupId}")
    public ResponseEntity<?> startRestore(@PathVariable String backupId) {
        try {
            String restoreId = restoreService.initiateRestore(backupId);
            return ResponseEntity.accepted().body(Map.of(
                    "message", "Restore initiated successfully",
                    "restoreId", restoreId
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> getRestoreStatus(@PathVariable String id) {
        Optional<RestoreLog> log = restoreRepository.findById(id);
        if (log.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        RestoreLog record = log.get();
        return ResponseEntity.ok(Map.of(
                "restoreId", record.getId(),
                "backupId", record.getBackupMetadata().getId(),
                "status", record.getStatus(),
                "time", record.getTime(),
                "details", record.getDetails()
        ));
    }
}