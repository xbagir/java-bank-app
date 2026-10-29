package ru.yandex.practicum.bank.accounts.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.accounts.config.ClientCredentialsInterceptor;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(RestClient.Builder restClientBuilder,
                              ClientCredentialsInterceptor clientCredentialsInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl("http://notifications")
                .requestInterceptor(clientCredentialsInterceptor)
                .build();
    }

    @CircuitBreaker(name = "notificationClient", fallbackMethod = "sendFallback")
    public boolean send(NotificationRequest request) {
        restClient.post()
                .uri("/notifications")
                .body(request)
                .retrieve()
                .toBodilessEntity();
        return true;
    }

    private boolean sendFallback(NotificationRequest request, Throwable throwable) {
        log.warn("Notifications service is unavailable: {}", throwable.getMessage());
        return false;
    }
}
