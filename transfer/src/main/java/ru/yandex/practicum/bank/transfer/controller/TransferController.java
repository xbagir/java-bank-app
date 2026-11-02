package ru.yandex.practicum.bank.transfer.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.transfer.dto.TransferRequest;
import ru.yandex.practicum.bank.transfer.dto.TransferResponse;
import ru.yandex.practicum.bank.transfer.service.TransferService;

import java.util.UUID;

@RestController
@RequestMapping("/transfer")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request,
                                     @RequestHeader("Idempotency-Key") UUID requestId,
                                     Authentication authentication) {
        return transferService.transfer(login(authentication), request.login(), request.value(),
                requestId);
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
