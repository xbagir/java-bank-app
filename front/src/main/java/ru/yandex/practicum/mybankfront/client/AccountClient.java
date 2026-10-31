package ru.yandex.practicum.mybankfront.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.config.UserTokenInterceptor;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.controller.dto.UpdateAccountRequest;

import java.time.LocalDate;
import java.util.List;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient.Builder restClientBuilder,
                         UserTokenInterceptor userTokenInterceptor,
                         @Value("${bank.gateway.base-url:http://gateway}") String gatewayBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(gatewayBaseUrl)
                .requestInterceptor(userTokenInterceptor)
                .build();
    }

    public AccountResponse getCurrentAccount() {
        return restClient.get()
                .uri("/api/accounts/me")
                .retrieve()
                .body(AccountResponse.class);
    }

    public AccountResponse updateCurrentAccount(String name, LocalDate birthdate) {
        return restClient.put()
                .uri("/api/accounts/me")
                .body(new UpdateAccountRequest(name, birthdate))
                .retrieve()
                .body(AccountResponse.class);
    }

    public List<AccountDto> getOtherAccounts() {
        return restClient.get()
                .uri("/api/accounts/others")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
