package ru.yandex.practicum.bank.accounts.kafka;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.accounts.client.KafkaNotificationPublisher;
import ru.yandex.practicum.bank.accounts.model.OutboxEvent;
import ru.yandex.practicum.bank.accounts.repository.OutboxEventRepository;
import ru.yandex.practicum.bank.accounts.service.OutboxPublisher;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OutboxPublisherTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private KafkaNotificationPublisher notificationPublisher;

    @Test
    void shouldKeepEventPendingUntilKafkaAcceptsIt() {
        when(notificationPublisher.send(any())).thenReturn(false);
        OutboxEvent event = outboxEventRepository.save(
                new OutboxEvent("ivanov", "CASH_DEPOSIT", "Счёт пополнен"));

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            outboxPublisher.publishPendingEvents();
            OutboxEvent current = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(current.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
            assertThat(current.getRetryCount()).isGreaterThanOrEqualTo(5);
        });

        when(notificationPublisher.send(any())).thenReturn(true);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            outboxPublisher.publishPendingEvents();
            assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
                    .isEqualTo(OutboxEvent.Status.SENT);
        });
    }
}
