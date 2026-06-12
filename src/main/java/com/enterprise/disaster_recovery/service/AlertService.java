package com.enterprise.disaster_recovery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    public void sendAlert(String subject, String message) {
        logger.error("!!! CRITICAL ALERT Triggered !!!\nSubject: {}\nMessage: {}", subject, message);
    }
}