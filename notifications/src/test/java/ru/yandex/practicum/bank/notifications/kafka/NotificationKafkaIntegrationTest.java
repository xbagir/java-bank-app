package ru.yandex.practicum.bank.notifications.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.bank.notifications.repository.NotificationRepository;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
class NotificationKafkaIntegrationTest {

    private static final String TOPIC = "bank.notifications";
    private static final String GROUP = "bank-notifications";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void clearRepository() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldConsumeMessageAndCommitOffset() throws Exception {
        UUID eventId = UUID.randomUUID();
        kafkaTemplate().send(TOPIC, eventId.toString(), message(eventId)).get(10, TimeUnit.SECONDS);

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(notificationRepository.findByEventId(eventId)).isPresent());
        assertThat(notificationRepository.count()).isEqualTo(1);

        var notification = notificationRepository.findByEventId(eventId).orElseThrow();
        assertThat(notification.getRecipient()).isEqualTo("ivanov");
        assertThat(notification.getType()).isEqualTo("CASH_DEPOSIT");
        assertThat(notification.getMessage()).isEqualTo("Счёт пополнен");

        try (Admin admin = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString()))) {
            Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                Map<TopicPartition, OffsetAndMetadata> offsets = admin
                        .listConsumerGroupOffsets(GROUP)
                        .partitionsToOffsetAndMetadata()
                        .get(10, TimeUnit.SECONDS);
                assertThat(offsets.values()).anySatisfy(value -> assertThat(value.offset()).isGreaterThan(0));
            });
        }
    }

    @Test
    void shouldIgnoreDuplicateEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        KafkaTemplate<String, String> template = kafkaTemplate();
        template.send(TOPIC, eventId.toString(), message(eventId)).get(10, TimeUnit.SECONDS);
        template.send(TOPIC, eventId.toString(), message(eventId)).get(10, TimeUnit.SECONDS);

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(notificationRepository.count()).isEqualTo(1));
        assertThat(notificationRepository.findByEventId(eventId)).isPresent();
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
