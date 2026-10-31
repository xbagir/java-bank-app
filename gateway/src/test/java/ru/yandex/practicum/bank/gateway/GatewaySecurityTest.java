package ru.yandex.practicum.bank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldServeFallbackWithoutAuthentication() {
        webTestClient.get()
                .uri("/fallback/accounts")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("Сервис 'accounts' временно недоступен");
    }

    @Test
    void shouldRequireTokenForApiRoutes() {
        webTestClient.get()
                .uri("/api/accounts/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRequireTokenForCashRoute() {
        webTestClient.post()
                .uri("/api/cash/deposit")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
