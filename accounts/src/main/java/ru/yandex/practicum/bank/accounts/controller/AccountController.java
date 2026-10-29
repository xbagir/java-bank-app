package ru.yandex.practicum.bank.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.AccountShortResponse;
import ru.yandex.practicum.bank.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.bank.accounts.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountResponse getCurrentAccount(Authentication authentication) {
        return accountService.getByLogin(login(authentication));
    }

    @PutMapping("/me")
    public AccountResponse updateCurrentAccount(@Valid @RequestBody UpdateAccountRequest request,
                                                Authentication authentication) {
        return accountService.updateProfile(login(authentication), request.name(), request.birthdate());
    }

    @GetMapping("/others")
    public List<AccountShortResponse> getOtherAccounts(Authentication authentication) {
        return accountService.getOtherAccounts(login(authentication));
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
