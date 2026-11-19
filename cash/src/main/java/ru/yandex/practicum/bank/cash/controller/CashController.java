package ru.yandex.practicum.bank.cash.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.cash.dto.CashRequest;
import ru.yandex.practicum.bank.cash.dto.CashResponse;
import ru.yandex.practicum.bank.cash.service.CashService;

import java.util.UUID;

@RestController
@RequestMapping("/cash")
public class CashController {

    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @PostMapping("/deposit")
    public CashResponse deposit(@Valid @RequestBody CashRequest request,
                                @RequestHeader("Idempotency-Key") UUID requestId,
                                Authentication authentication) {
        return cashService.deposit(login(authentication), request.value(), requestId);
    }

    @PostMapping("/withdraw")
    public CashResponse withdraw(@Valid @RequestBody CashRequest request,
                                 @RequestHeader("Idempotency-Key") UUID requestId,
                                 Authentication authentication) {
        return cashService.withdraw(login(authentication), request.value(), requestId);
    }

    private String login(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String preferredUsername = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
        }
        return authentication.getName();
    }
}
