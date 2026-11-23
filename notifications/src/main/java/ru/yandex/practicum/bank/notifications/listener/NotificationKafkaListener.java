package ru.yandex.practicum.bank.notifications.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.service.NotificationService;

@Component
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    public NotificationKafkaListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${kafka.topic.notifications:bank.notifications}")
    public void consume(NotificationRequest request, Acknowledgment acknowledgment) {
        notificationService.send(request);
        acknowledgment.acknowledge();
    }
}
