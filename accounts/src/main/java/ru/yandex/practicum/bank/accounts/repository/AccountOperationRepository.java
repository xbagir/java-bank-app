package ru.yandex.practicum.bank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.bank.accounts.model.AccountOperation;

import java.util.Optional;
import java.util.UUID;

public interface AccountOperationRepository extends JpaRepository<AccountOperation, Long> {

    Optional<AccountOperation> findByRequestId(UUID requestId);
}
