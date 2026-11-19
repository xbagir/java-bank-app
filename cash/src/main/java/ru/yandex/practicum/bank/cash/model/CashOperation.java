package ru.yandex.practicum.bank.cash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_operations")
public class CashOperation {

    public enum Type {
        DEPOSIT, WITHDRAW
    }

    public enum Status {
        PENDING, SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true)
    private UUID requestId;

    @Column(nullable = false, length = 100)
    private String login;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CashOperation() {
    }

    public CashOperation(UUID requestId, String login, Type type, BigDecimal amount) {
        this.requestId = requestId;
        this.login = login;
        this.type = type;
        this.amount = amount;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void markSuccess(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
        this.failureReason = null;
        this.status = Status.SUCCESS;
    }

    public void markFailed(String failureReason) {
        this.failureReason = failureReason;
        this.status = Status.FAILED;
    }

    public Long getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public String getLogin() {
        return login;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
