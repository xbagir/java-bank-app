package ru.yandex.practicum.bank.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaNotificationPublisher {

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private final String topic;

    public KafkaNotificationPublisher(KafkaTemplate<String, NotificationRequest> kafkaTemplate,
                                      @Value("${kafka.topic.notifications:bank.notifications}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public boolean send(NotificationRequest request) {
        try {
            kafkaTemplate.send(topic, request.eventId().toString(), request).get(10, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception exception) {
            return false;
        }
    }
}
