package ru.yandex.practicum.bank.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.bank.transfer.model.OutboxEvent;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "select * from outbox_events where status = 'PENDING' "
            + "order by id limit :limit for update skip locked", nativeQuery = true)
    List<OutboxEvent> claimBatch(@Param("limit") int limit);

    @Modifying
    @Query(value = "update outbox_events set status = 'PENDING', locked_at = null "
            + "where status = 'PROCESSING' and locked_at < :threshold", nativeQuery = true)
    int resetStuck(@Param("threshold") Instant threshold);
}
