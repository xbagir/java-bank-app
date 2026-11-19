package ru.yandex.practicum.bank.accounts.model;

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
@Table(name = "account_operations")
public class AccountOperation {

    public enum Type {
        DEPOSIT, WITHDRAW, TRANSFER
    }

    public enum Status {
        PROCESSING, SUCCESS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private Type type;

    @Column(name = "from_login", nullable = false, length = 100)
    private String fromLogin;

    @Column(name = "to_login", length = 100)
    private String toLogin;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountOperation() {
    }

    public AccountOperation(UUID requestId, Type type, String fromLogin, String toLogin,
                            BigDecimal amount) {
        this.requestId = requestId;
        this.type = type;
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
        this.amount = amount;
        this.status = Status.PROCESSING;
        this.createdAt = Instant.now();
    }

    public void complete(BigDecimal balanceBefore, BigDecimal balanceAfter) {
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.status = Status.SUCCESS;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public Type getType() {
        return type;
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

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
