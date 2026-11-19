package ru.yandex.practicum.bank.cash.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.cash.config.ClientCredentialsInterceptor;
import ru.yandex.practicum.bank.cash.dto.AccountResponse;
import ru.yandex.practicum.bank.cash.dto.AmountRequest;
import ru.yandex.practicum.bank.cash.exception.AccountsServiceUnavailableException;
import ru.yandex.practicum.bank.cash.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountsClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);

    private final RestClient restClient;

    public AccountsClient(RestClient.Builder restClientBuilder,
                          ClientCredentialsInterceptor clientCredentialsInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl("http://accounts")
                .requestInterceptor(clientCredentialsInterceptor)
                .build();
    }

    @CircuitBreaker(name = "accountsClient", fallbackMethod = "depositFallback")
    public AccountResponse deposit(String login, BigDecimal amount, UUID requestId) {
        try {
            return restClient.post()
                    .uri("/internal/accounts/{login}/deposit", login)
                    .body(new AmountRequest(requestId, amount))
                    .retrieve()
                    .body(AccountResponse.class);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new InsufficientFundsException();
        }
    }

    @CircuitBreaker(name = "accountsClient", fallbackMethod = "withdrawFallback")
    public AccountResponse withdraw(String login, BigDecimal amount, UUID requestId) {
        try {
            return restClient.post()
                    .uri("/internal/accounts/{login}/withdraw", login)
                    .body(new AmountRequest(requestId, amount))
                    .retrieve()
                    .body(AccountResponse.class);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new InsufficientFundsException();
        }
    }

    private AccountResponse depositFallback(String login, BigDecimal amount, UUID requestId, Throwable throwable) {
        return fallback(throwable);
    }

    private AccountResponse withdrawFallback(String login, BigDecimal amount, UUID requestId, Throwable throwable) {
        return fallback(throwable);
    }

    private AccountResponse fallback(Throwable throwable) {
        if (throwable instanceof InsufficientFundsException insufficientFundsException) {
            throw insufficientFundsException;
        }
        log.warn("Accounts service is unavailable: {}", throwable.getMessage());
        throw new AccountsServiceUnavailableException(throwable);
    }
}
