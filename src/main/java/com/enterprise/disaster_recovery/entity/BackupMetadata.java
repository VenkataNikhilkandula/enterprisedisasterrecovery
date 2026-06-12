package com.enterprise.disaster_recovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_metadata")
public class BackupMetadata {
    @Id
    @Column(length = 36)
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupType type;
    @Column(nullable = false)
    private LocalDateTime timestamp;
    @Column(length = 255)
    private String location;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupStatus status;
    @Column(length = 256)
    private String checksum;
    @Column(name = "size_bytes")
    private Long sizeBytes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BackupType getType() { return type; }
    public void setType(BackupType type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}