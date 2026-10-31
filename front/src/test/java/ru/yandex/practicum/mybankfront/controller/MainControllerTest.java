package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.controller.dto.TransferResponse;
import ru.yandex.practicum.mybankfront.service.BankService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MainController.class)
@AutoConfigureMockMvc(addFilters = false)
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankService bankService;

    @Test
    void shouldRenderMainPageWithAccountData() throws Exception {
        when(bankService.getAccount()).thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), new BigDecimal("1000.00")));
        when(bankService.getOtherAccounts()).thenReturn(List.of(
                new AccountDto("petrov", "Петров Пётр")));

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("name", "Иванов Иван"))
                .andExpect(model().attribute("birthdate", "2001-01-01"))
                .andExpect(model().attribute("sum", new BigDecimal("1000.00")))
                .andExpect(model().attributeExists("accounts"));
    }

    @Test
    void shouldRenderSuccessAfterTransfer() throws Exception {
        when(bankService.transfer(any(), any(), any())).thenReturn(new TransferResponse(
                "ivanov", "petrov", new BigDecimal("100.00"),
                new BigDecimal("900.00"), "Переведено 100 руб."));
        when(bankService.getAccount()).thenReturn(new AccountResponse("ivanov", "Иванов Иван",
                LocalDate.of(2001, 1, 1), new BigDecimal("900.00")));
        when(bankService.getOtherAccounts()).thenReturn(List.of(
                new AccountDto("petrov", "Петров Пётр")));

        mockMvc.perform(post("/transfer")
                        .param("login", "petrov")
                        .param("value", "100.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Переведено 100 руб."));
    }
}
