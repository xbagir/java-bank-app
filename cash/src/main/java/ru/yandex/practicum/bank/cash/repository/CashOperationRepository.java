package ru.yandex.practicum.bank.cash.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.bank.cash.model.CashOperation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

    Optional<CashOperation> findByRequestId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CashOperation o where o.requestId = :requestId")
    Optional<CashOperation> findByRequestIdForUpdate(@Param("requestId") UUID requestId);

    List<CashOperation> findTop20ByStatusAndCreatedAtBeforeOrderByIdAsc(
            CashOperation.Status status, Instant createdAt);
}
