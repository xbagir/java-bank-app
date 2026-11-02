package ru.yandex.practicum.bank.accounts.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.accounts.config.SecurityConfig;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.TransferResponse;
import ru.yandex.practicum.bank.accounts.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Внутренний API доступен только сервисным токенам (SCOPE_service)
 * и требует соответствующих привилегий на операцию.
 */
@WebMvcTest(InternalAccountController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = "bank.security.enabled=true")
class InternalAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(username = "cash-service",
            authorities = {"SCOPE_service", "SCOPE_accounts.read", "SCOPE_accounts.write"})
    void shouldAllowServiceToDepositMoney() throws Exception {
        when(accountService.deposit(any(), any(), any()))
                .thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                        LocalDate.of(2001, 1, 1), new BigDecimal("1100.00")));

        mockMvc.perform(post("/internal/accounts/ivanov/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"%s","amount":100.00}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1100.00));
    }

    @Test
    @WithMockUser(username = "cash-service",
            authorities = {"SCOPE_service", "SCOPE_accounts.read", "SCOPE_accounts.write"})
    void shouldAllowServiceToReadAccounts() throws Exception {
        when(accountService.getByLogin("ivanov"))
                .thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                        LocalDate.of(2001, 1, 1), new BigDecimal("1000.00")));

        mockMvc.perform(get("/internal/accounts/ivanov"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ivanov"));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = {"SCOPE_accounts.read", "SCOPE_accounts.write"})
    void shouldForbidUserTokenOnInternalEndpoint() throws Exception {
        mockMvc.perform(get("/internal/accounts/ivanov"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cash-service",
            authorities = {"SCOPE_service", "SCOPE_accounts.read", "SCOPE_accounts.write"})
    void shouldForbidTransferWithoutTransferScope() throws Exception {
        mockMvc.perform(post("/internal/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"%s","fromLogin":"ivanov","toLogin":"petrov","amount":100.00}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "transfer-service",
            authorities = {"SCOPE_service", "SCOPE_accounts.read", "SCOPE_accounts.transfer"})
    void shouldAllowTransferServiceToTransferMoney() throws Exception {
        when(accountService.transfer(any(), any(), any(), any()))
                .thenReturn(new TransferResponse("ivanov", "petrov", new BigDecimal("100.00"),
                        new BigDecimal("900.00")));

        mockMvc.perform(post("/internal/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"%s","fromLogin":"ivanov","toLogin":"petrov","amount":100.00}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromLogin").value("ivanov"))
                .andExpect(jsonPath("$.balance").value(900.00));
    }
}
