package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.service.BankService;
import ru.yandex.practicum.mybankfront.service.MainPageModelFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.when;

/**
 * Проверяет, что формы main.html содержат CSRF-токен и что POST-запросы
 * без токена отклоняются, а некорректные данные показываются на странице.
 */
@WebMvcTest(MainController.class)
@AutoConfigureMockMvc
@Import({MainPageModelFactory.class, MainPageCsrfTest.PermissiveSecurityConfig.class})
class MainPageCsrfTest {

    @TestConfiguration
    static class PermissiveSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankService bankService;

    @BeforeEach
    void setUp() {
        when(bankService.getAccount()).thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), new BigDecimal("1000.00")));
        when(bankService.getOtherAccounts()).thenReturn(List.of(
                new AccountDto("petrov", "Петров Пётр")));
    }

    @Test
    void shouldRenderCsrfTokenInEveryForm() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("action=\"/account\"")))
                .andExpect(content().string(containsString("action=\"/cash\"")))
                .andExpect(content().string(containsString("action=\"/transfer\"")));
    }

    @Test
    void shouldRejectPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/account")
                        .param("name", "Иванов Иван")
                        .param("birthdate", "2000-01-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptPostWithCsrfToken() throws Exception {
        when(bankService.updateAccount("Иванов Иван", LocalDate.of(2000, 1, 1)))
                .thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                        LocalDate.of(2000, 1, 1), new BigDecimal("1000.00")));

        mockMvc.perform(post("/account").with(csrf())
                        .param("name", "Иванов Иван")
                        .param("birthdate", "2000-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Данные профиля сохранены"));
    }

    @Test
    void shouldShowErrorForNonNumericAmount() throws Exception {
        mockMvc.perform(post("/cash").with(csrf())
                        .param("value", "abc")
                        .param("action", "PUT"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("errors", List.of("Сумма должна быть числом")));
    }

    @Test
    void shouldShowErrorForMissingBirthdate() throws Exception {
        mockMvc.perform(post("/account").with(csrf())
                        .param("name", "Иванов Иван"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("errors",
                        List.of("Дата рождения обязательна для заполнения")));
    }
}
