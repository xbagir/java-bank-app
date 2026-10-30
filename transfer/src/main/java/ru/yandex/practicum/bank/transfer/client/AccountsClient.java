package ru.yandex.practicum.bank.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.transfer.config.ClientCredentialsInterceptor;
import ru.yandex.practicum.bank.transfer.dto.AccountTransferResponse;
import ru.yandex.practicum.bank.transfer.dto.AccountsTransferRequest;
import ru.yandex.practicum.bank.transfer.exception.AccountsServiceUnavailableException;
import ru.yandex.practicum.bank.transfer.exception.InsufficientFundsException;

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

    @CircuitBreaker(name = "accountsClient", fallbackMethod = "transferFallback")
    public AccountTransferResponse transfer(String fromLogin, String toLogin, BigDecimal amount,
                                                UUID requestId) {
        try {
            return restClient.post()
                    .uri("/internal/accounts/transfer")
                    .body(new AccountsTransferRequest(requestId, fromLogin, toLogin, amount))
                    .retrieve()
                    .body(AccountTransferResponse.class);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new InsufficientFundsException();
        }
    }

    private AccountTransferResponse transferFallback(String fromLogin, String toLogin,
                                                     BigDecimal amount, UUID requestId,
                                                     Throwable throwable) {
        if (throwable instanceof InsufficientFundsException insufficientFundsException) {
            throw insufficientFundsException;
        }
        log.warn("Accounts service is unavailable: {}", throwable.getMessage());
        throw new AccountsServiceUnavailableException(throwable);
    }
}
