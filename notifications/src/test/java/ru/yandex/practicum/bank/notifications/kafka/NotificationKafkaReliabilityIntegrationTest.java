package ru.yandex.practicum.bank.notifications.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.service.NotificationService;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
@TestPropertySource(properties = "kafka.consumer.group-id=bank-notifications-reliability")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationKafkaReliabilityIntegrationTest {

    private static final String TOPIC = "bank.notifications";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void shouldRetryUntilNotificationIsStored() throws Exception {
        doThrow(new IllegalStateException("database is unavailable"))
                .doThrow(new IllegalStateException("database is unavailable"))
                .doReturn(null)
                .when(notificationService).send(any());

        send(UUID.randomUUID());

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                verify(notificationService, times(3)).send(any()));
    }

    @Test
    void shouldResumeFromCommittedOffsetAfterRestart() throws Exception {
        doReturn(null).when(notificationService).send(any());

        UUID first = UUID.randomUUID();
        send(first);
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                verify(notificationService, times(1)).send(any()));

        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::stop);

        UUID second = UUID.randomUUID();
        send(second);
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                verify(notificationService, atLeast(2)).send(any()));
        Awaitility.await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(notificationService, times(2)).send(any()));

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(2)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationRequest::eventId)
                .containsExactlyInAnyOrder(first, second);
    }

    private void send(UUID eventId) throws Exception {
        kafkaTemplate().send(TOPIC, eventId.toString(), message(eventId))
                .get(10, TimeUnit.SECONDS);
    }

    private KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString()),
                new StringSerializer(), new StringSerializer()));
    }

    private String message(UUID eventId) {
        return """
                {"eventId":"%s","recipient":"ivanov","type":"CASH_DEPOSIT","message":"Счёт пополнен"}
                """.formatted(eventId);
    }
}
