package com.enterprise.disaster_recovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restore_logs")
public class RestoreLog {
    @Id
    @Column(length = 36)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_id", nullable = false)
    private BackupMetadata backupMetadata;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupStatus status;
    @Column(nullable = false)
    private LocalDateTime time;
    @Column(columnDefinition = "TEXT")
    private String details;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BackupMetadata getBackupMetadata() { return backupMetadata; }
    public void setBackupMetadata(BackupMetadata backupMetadata) { this.backupMetadata = backupMetadata; }
    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}