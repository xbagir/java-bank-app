package ru.yandex.practicum.bank.accounts.contract;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import io.restassured.RestAssured;
import ru.yandex.practicum.bank.accounts.model.Account;
import ru.yandex.practicum.bank.accounts.repository.AccountRepository;
import ru.yandex.practicum.bank.accounts.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class AccountsContractBase {

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUpAccounts() {
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(new Account("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), new BigDecimal("1000.00")));
        accountRepository.save(new Account("petrov", "Петров Пётр",
                LocalDate.of(1995, 5, 15), new BigDecimal("500.00")));
    }
}
