package ru.yandex.practicum.bank.notifications.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.dto.NotificationResponse;
import ru.yandex.practicum.bank.notifications.service.NotificationService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void shouldAcceptNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationService.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationResponse(1L, eventId, "ivanov", "CASH_DEPOSIT",
                        "Пополнение", Instant.parse("2026-11-01T10:00:00Z")));

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":"%s","recipient":"ivanov","type":"CASH_DEPOSIT","message":"Пополнение"}
                                """.formatted(eventId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.recipient").value("ivanov"));
    }
}
