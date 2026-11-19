package ru.yandex.practicum.bank.transfer.controller;

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
import ru.yandex.practicum.bank.transfer.config.SecurityConfig;
import ru.yandex.practicum.bank.transfer.dto.TransferResponse;
import ru.yandex.practicum.bank.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.bank.transfer.exception.InvalidTransferException;
import ru.yandex.practicum.bank.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = "bank.security.enabled=true")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_transfer.write")
    void shouldTransferMoney() throws Exception {
        when(transferService.transfer(any(), any(), any(), any()))
                .thenReturn(new TransferResponse("ivanov", "petrov", new BigDecimal("100.00"),
                        new BigDecimal("900.00"), "Переведено"));

        mockMvc.perform(post("/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"petrov","value":100.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toLogin").value("petrov"));
    }

    @Test
    void shouldRejectRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"petrov","value":100.00}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_cash.write")
    void shouldForbidTransferWithoutTransferScope() throws Exception {
        mockMvc.perform(post("/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"petrov","value":100.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_transfer.write")
    void shouldReturnConflictWhenBalanceIsInsufficient() throws Exception {
        when(transferService.transfer(any(), any(), any(), any()))
                .thenThrow(new InsufficientFundsException());

        mockMvc.perform(post("/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"petrov","value":5000.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Недостаточно средств на счету"));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_transfer.write")
    void shouldReturnBadRequestForTransferToSameAccount() throws Exception {
        when(transferService.transfer(any(), any(), any(), any()))
                .thenThrow(new InvalidTransferException("Нельзя перевести деньги на свой же счёт"));

        mockMvc.perform(post("/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"ivanov","value":100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Нельзя перевести деньги на свой же счёт"));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_transfer.write")
    void shouldReturnBadRequestForNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"petrov","value":-100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_transfer.write")
    void shouldReturnBadRequestWhenIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"petrov","value":100.00}
                                """))
                .andExpect(status().isBadRequest());
    }
}
