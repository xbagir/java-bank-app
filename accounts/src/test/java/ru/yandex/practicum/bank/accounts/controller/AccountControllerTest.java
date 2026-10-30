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
import ru.yandex.practicum.bank.accounts.dto.AccountShortResponse;
import ru.yandex.practicum.bank.accounts.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = "bank.security.enabled=true")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_accounts.read")
    void shouldReturnCurrentAccount() throws Exception {
        when(accountService.getByLogin("ivanov"))
                .thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                        LocalDate.of(2001, 1, 1), new BigDecimal("1000.00")));

        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ivanov"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_accounts.write")
    void shouldUpdateCurrentAccount() throws Exception {
        when(accountService.updateProfile(any(), any(), any()))
                .thenReturn(new AccountResponse("ivanov", "Иванов Пётр",
                        LocalDate.of(2000, 2, 2), new BigDecimal("1000.00")));

        mockMvc.perform(put("/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иванов Пётр","birthdate":"2000-02-02"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Иванов Пётр"));
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_accounts.read")
    void shouldReturnOtherAccounts() throws Exception {
        when(accountService.getOtherAccounts("ivanov"))
                .thenReturn(List.of(new AccountShortResponse("petrov", "Петров Пётр")));

        mockMvc.perform(get("/accounts/others"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("petrov"));
    }

    @Test
    void shouldRejectRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_accounts.read")
    void shouldForbidProfileUpdateWithoutWriteScope() throws Exception {
        mockMvc.perform(put("/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иванов Пётр","birthdate":"2000-02-02"}
                                """))
                .andExpect(status().isForbidden());
    }
}
