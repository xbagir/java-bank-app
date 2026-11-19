package ru.yandex.practicum.bank.transfer.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.bank.transfer.model.Transfer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByRequestId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.requestId = :requestId")
    Optional<Transfer> findByRequestIdForUpdate(@Param("requestId") UUID requestId);

    List<Transfer> findTop20ByStatusAndCreatedAtBeforeOrderByIdAsc(
            Transfer.Status status, Instant createdAt);
}
