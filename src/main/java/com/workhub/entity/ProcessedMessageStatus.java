package com.workhub.entity;

/**
 * Persistent processing state for consumed integration events.
 */
public enum ProcessedMessageStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
