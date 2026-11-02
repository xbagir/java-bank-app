package ru.yandex.practicum.mybankfront.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.config.UserTokenInterceptor;
import ru.yandex.practicum.mybankfront.controller.dto.CashRequest;
import ru.yandex.practicum.mybankfront.controller.dto.CashResponse;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CashClient {

    private final RestClient restClient;

    public CashClient(RestClient.Builder restClientBuilder,
                      UserTokenInterceptor userTokenInterceptor,
                         @Value("${bank.gateway.base-url:http://gateway}") String gatewayBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(gatewayBaseUrl)
                .requestInterceptor(userTokenInterceptor)
                .build();
    }

    public CashResponse deposit(BigDecimal value, UUID requestId) {
        return restClient.post()
                .uri("/api/cash/deposit")
                .header("Idempotency-Key", requestId.toString())
                .body(new CashRequest(value))
                .retrieve()
                .body(CashResponse.class);
    }

    public CashResponse withdraw(BigDecimal value, UUID requestId) {
        return restClient.post()
                .uri("/api/cash/withdraw")
                .header("Idempotency-Key", requestId.toString())
                .body(new CashRequest(value))
                .retrieve()
                .body(CashResponse.class);
    }
}
