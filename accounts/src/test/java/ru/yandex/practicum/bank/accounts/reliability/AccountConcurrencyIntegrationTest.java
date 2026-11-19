package ru.yandex.practicum.bank.accounts.reliability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.practicum.bank.accounts.service.AccountService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class AccountConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("outbox.publish-delay", () -> "60000");
        registry.add("outbox.recovery-delay", () -> "60000");
    }

    @Autowired
    private AccountService accountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetBalances() {
        jdbcTemplate.update("update accounts set balance = 1000.00 where login = 'ivanov'");
        jdbcTemplate.update("update accounts set balance = 500.00 where login = 'petrov'");
    }

    @Test
    void shouldApplyDepositOnlyOnceForDuplicateRequestId() {
        UUID requestId = UUID.randomUUID();

        accountService.deposit("ivanov", new BigDecimal("100.00"), requestId);
        accountService.deposit("ivanov", new BigDecimal("100.00"), requestId);

        assertThat(balanceOf("ivanov")).isEqualByComparingTo("1100.00");
    }

    @Test
    void shouldApplyConcurrentDuplicateDepositOnlyOnce() throws Exception {
        UUID requestId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    accountService.deposit("ivanov", new BigDecimal("100.00"), requestId);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(balanceOf("ivanov")).isEqualByComparingTo("1100.00");
    }

    @Test
    void shouldNotLoseMoneyUnderOppositeConcurrentTransfers() throws Exception {
        int iterations = 3;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> transferLoop("ivanov", "petrov", iterations,
                ready, start)));
        futures.add(executor.submit(() -> transferLoop("petrov", "ivanov", iterations,
                ready, start)));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(balanceOf("ivanov")).isEqualByComparingTo("1000.00");
        assertThat(balanceOf("petrov")).isEqualByComparingTo("500.00");
    }

    private void transferLoop(String from, String to, int iterations, CountDownLatch ready,
                              CountDownLatch start) {
        try {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            for (int i = 0; i < iterations; i++) {
                accountService.transfer(from, to, new BigDecimal("10.00"), UUID.randomUUID());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private BigDecimal balanceOf(String login) {
        return jdbcTemplate.queryForObject(
                "select balance from accounts where login = ?", BigDecimal.class, login);
    }
}
