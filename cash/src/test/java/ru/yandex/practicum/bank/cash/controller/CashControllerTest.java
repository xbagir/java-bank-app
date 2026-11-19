package ru.yandex.practicum.bank.cash.controller;

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
import ru.yandex.practicum.bank.cash.config.SecurityConfig;
import ru.yandex.practicum.bank.cash.dto.CashResponse;
import ru.yandex.practicum.bank.cash.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.cash.exception.InvalidCashOperationException;
import ru.yandex.practicum.bank.cash.service.CashService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = "bank.security.enabled=true")
class CashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_cash.write")
    void shouldDepositMoney() throws Exception {
        when(cashService.deposit(any(), any(), any()))
                .thenReturn(new CashResponse("ivanov", "PUT", new BigDecimal("100.00"),
                        new BigDecimal("1100.00"), "Счёт пополнен"));

        mockMvc.perform(post("/cash/deposit")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":100.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1100.00));
    }

    @Test
    void shouldRejectRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/cash/deposit")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":100.00}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_accounts.read")
    void shouldForbidCashOperationWithoutCashScope() throws Exception {
        mockMvc.perform(post("/cash/deposit")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":100.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_cash.write")
    void shouldReturnConflictWhenBalanceIsInsufficient() throws Exception {
        when(cashService.withdraw(any(), any(), any()))
                .thenThrow(new InsufficientFundsException());

        mockMvc.perform(post("/cash/withdraw")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":5000.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Недостаточно средств на счету"));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_cash.write")
    void shouldReturnBadRequestForNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/cash/deposit")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":-100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_cash.write")
    void shouldReturnBadRequestWhenIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post("/cash/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":100.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_cash.write")
    void shouldReturnBadRequestForInvalidOperation() throws Exception {
        when(cashService.deposit(any(), any(), any()))
                .thenThrow(new InvalidCashOperationException(
                        "Сумма должна содержать не более 2 знаков после запятой"));

        mockMvc.perform(post("/cash/deposit")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":100.001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Сумма должна содержать не более 2 знаков после запятой"));
    }
}
