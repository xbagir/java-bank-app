package ru.yandex.practicum.bank.transfer.model;

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
@Table(name = "transfers")
public class Transfer {

    public enum Status {
        PENDING, SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true)
    private UUID requestId;

    @Column(name = "from_login", nullable = false, length = 100)
    private String fromLogin;

    @Column(name = "to_login", nullable = false, length = 100)
    private String toLogin;

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

    protected Transfer() {
    }

    public Transfer(UUID requestId, String fromLogin, String toLogin, BigDecimal amount) {
        this.requestId = requestId;
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
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

    public String getFromLogin() {
        return fromLogin;
    }

    public String getToLogin() {
        return toLogin;
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
