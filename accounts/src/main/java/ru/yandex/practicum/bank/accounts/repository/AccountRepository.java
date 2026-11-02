package ru.yandex.practicum.bank.accounts.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.bank.accounts.model.Account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByLogin(String login);

    List<Account> findAllByLoginNotOrderByNameAsc(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.login = :login")
    Optional<Account> findByLoginForUpdate(@Param("login") String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.login in :logins order by a.login")
    List<Account> findAllByLoginInForUpdate(@Param("logins") Collection<String> logins);
}
