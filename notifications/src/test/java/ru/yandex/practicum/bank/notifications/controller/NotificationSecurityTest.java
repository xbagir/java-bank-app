package ru.yandex.practicum.bank.notifications.controller;

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
import ru.yandex.practicum.bank.notifications.config.SecurityConfig;
import ru.yandex.practicum.bank.notifications.dto.NotificationRequest;
import ru.yandex.practicum.bank.notifications.dto.NotificationResponse;
import ru.yandex.practicum.bank.notifications.service.NotificationService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Уведомления принимаются только от сервисов с привилегией notifications.write.
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = "bank.security.enabled=true")
class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldRejectNotificationWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ivanov", authorities = "SCOPE_accounts.read")
    void shouldForbidUserTokenWithoutNotificationScope() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cash-service", authorities = "SCOPE_notifications.write")
    void shouldAcceptServiceNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationService.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationResponse(1L, eventId, "ivanov", "CASH_DEPOSIT",
                        "Пополнение", Instant.parse("2026-11-01T10:00:00Z")));

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(eventId)))
                .andExpect(status().isAccepted());
    }

    private String body(UUID eventId) {
        return """
                {"eventId":"%s","recipient":"ivanov","type":"CASH_DEPOSIT","message":"Пополнение"}
                """.formatted(eventId);
    }
}
