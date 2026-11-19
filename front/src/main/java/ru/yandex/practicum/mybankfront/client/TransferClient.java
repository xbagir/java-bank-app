package ru.yandex.practicum.mybankfront.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.config.UserTokenInterceptor;
import ru.yandex.practicum.mybankfront.controller.dto.TransferRequest;
import ru.yandex.practicum.mybankfront.controller.dto.TransferResponse;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TransferClient {

    private final RestClient restClient;

    public TransferClient(RestClient.Builder restClientBuilder,
                          UserTokenInterceptor userTokenInterceptor,
                         @Value("${bank.gateway.base-url:http://gateway}") String gatewayBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(gatewayBaseUrl)
                .requestInterceptor(userTokenInterceptor)
                .build();
    }

    public TransferResponse transfer(String login, BigDecimal value, UUID requestId) {
        return restClient.post()
                .uri("/api/transfer")
                .header("Idempotency-Key", requestId.toString())
                .body(new TransferRequest(login, value))
                .retrieve()
                .body(TransferResponse.class);
    }
}
