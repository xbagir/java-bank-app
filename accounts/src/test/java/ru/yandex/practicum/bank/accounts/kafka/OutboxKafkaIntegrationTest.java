package ru.yandex.practicum.bank.accounts.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.bank.accounts.model.OutboxEvent;
import ru.yandex.practicum.bank.accounts.repository.OutboxEventRepository;
import ru.yandex.practicum.bank.accounts.service.OutboxPublisher;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
class OutboxKafkaIntegrationTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void shouldPublishOutboxEventToKafkaAndMarkItSent() {
        OutboxEvent event = outboxEventRepository.save(
                new OutboxEvent("ivanov", "CASH_DEPOSIT", "Счёт пополнен"));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "outbox-test-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Consumer<String, String> consumer =
                new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "bank.notifications");

        outboxPublisher.publishPendingEvents();

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, "bank.notifications", Duration.ofSeconds(15));
        assertThat(record.key()).isEqualTo(event.getEventId().toString());
        assertThat(record.value()).contains(event.getEventId().toString(), "CASH_DEPOSIT", "Счёт пополнен");
        assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxEvent.Status.SENT);
        consumer.close();
    }
}
