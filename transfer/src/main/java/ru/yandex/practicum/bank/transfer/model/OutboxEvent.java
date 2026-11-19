package ru.yandex.practicum.bank.transfer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public enum Status {
        PENDING, PROCESSING, SENT, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String recipient;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(String recipient, String type, String message) {
        this.eventId = UUID.randomUUID();
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.status = Status.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markProcessing() {
        this.status = Status.PROCESSING;
        this.lockedAt = Instant.now();
    }

    public void markSent() {
        this.status = Status.SENT;
        this.processedAt = Instant.now();
        this.lockedAt = null;
    }

    public void recordFailure(int maxAttempts, String error) {
        this.retryCount++;
        this.lastError = error;
        this.lockedAt = null;
        if (this.retryCount >= maxAttempts) {
            this.status = Status.FAILED;
            this.processedAt = Instant.now();
        } else {
            this.status = Status.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public String getLastError() {
        return lastError;
    }
}
