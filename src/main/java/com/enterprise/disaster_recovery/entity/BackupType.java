package com.enterprise.disaster_recovery.entity;

public enum BackupType {
    FULL,
    INCREMENTAL,
    DB_ONLY,
    FILES_ONLY
}