package ru.yandex.practicum.bank.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.accounts.dto.AccountResponse;
import ru.yandex.practicum.bank.accounts.dto.AmountRequest;
import ru.yandex.practicum.bank.accounts.dto.InternalTransferRequest;
import ru.yandex.practicum.bank.accounts.dto.TransferResponse;
import ru.yandex.practicum.bank.accounts.service.AccountService;

@RestController
@RequestMapping("/internal/accounts")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{login}")
    @PreAuthorize("hasAuthority('SCOPE_service') and "
            + "(hasAuthority('SCOPE_accounts.read') or hasAuthority('SCOPE_accounts.write') "
            + "or hasAuthority('SCOPE_accounts.transfer'))")
    public AccountResponse getByLogin(@PathVariable String login) {
        return accountService.getByLogin(login);
    }

    @PostMapping("/{login}/deposit")
    @PreAuthorize("hasAuthority('SCOPE_service') and hasAuthority('SCOPE_accounts.write')")
    public AccountResponse deposit(@PathVariable String login, @Valid @RequestBody AmountRequest request) {
        return accountService.deposit(login, request.amount(), request.requestId());
    }

    @PostMapping("/{login}/withdraw")
    @PreAuthorize("hasAuthority('SCOPE_service') and hasAuthority('SCOPE_accounts.write')")
    public AccountResponse withdraw(@PathVariable String login, @Valid @RequestBody AmountRequest request) {
        return accountService.withdraw(login, request.amount(), request.requestId());
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('SCOPE_service') and hasAuthority('SCOPE_accounts.transfer')")
    public TransferResponse transfer(@Valid @RequestBody InternalTransferRequest request) {
        return accountService.transfer(request.fromLogin(), request.toLogin(), request.amount(),
                request.requestId());
    }
}
