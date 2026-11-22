# Bank App — микросервисное приложение «Банк»

Учебное приложение на Java 21 / Spring Boot 3.5 / Spring Cloud 2025.
Пользователь входит через Keycloak, редактирует профиль, пополняет и снимает деньги,
переводит средства другому клиенту.

## Как устроено

- `front` — HTML-интерфейс, OAuth2 Authorization Code Flow.
- `gateway` — единая точка входа, проверка JWT и маршрутизация в сервисы.
- `accounts` — профиль, баланс и внутренние операции со счётом.
- `cash` — пополнение и снятие наличных.
- `transfer` — переводы между счетами.
- `notifications` — Kafka-консьюмер уведомлений, без HTTP API.
- `discovery-server` и `config-server` — Eureka и внешняя конфигурация.

Cash и Transfer вызывают Accounts по REST с Client Credentials. Уведомления
Accounts, Cash и Transfer передают через Transactional Outbox в Kafka topic
`bank.notifications`, откуда их читает Notifications.

### Модули

| Модуль | Назначение | Порт |
|---|---|---:|
| `discovery-server` | Eureka Server | 8761 |
| `config-server` | Spring Cloud Config Server, native-профиль | 8888 |
| `gateway` | Spring Cloud Gateway, JWT, Circuit Breaker | 8081 |
| `front` | Spring MVC + Thymeleaf, OAuth2 Login | 8080 |
| `accounts` | аккаунты, баланс, профиль | 8082 |
| `cash` | пополнение и снятие | 8083 |
| `transfer` | переводы | 8084 |
| `notifications` | Kafka-консьюмер, лог и БД | — |

## Требования

- JDK 21.
- Docker и Docker Compose.
- Maven не обязателен: используется `./mvnw`.
- Helm 3 и kubectl — только для запуска в Kubernetes.

Если локально установлен JDK старше 21, сборку и тесты можно выполнить в контейнере:

```bash
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp \
  -v "$PWD":/workspace -v "$PWD/.m2-bank":/tmp/m2 -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -Dmaven.repo.local=/tmp/m2/repository clean verify
```

## Быстрый запуск

Самый простой способ — Docker Compose:

```bash
docker compose up -d --build
```

Первый запуск занимает несколько минут: собираются образы и инициализируются
PostgreSQL, Keycloak и Kafka.

После запуска откройте:

- Front UI: http://localhost:8080
- Keycloak: http://localhost:8180 (admin / admin)
- Eureka: http://localhost:8761
- Config Server: http://localhost:8888
- Gateway: http://localhost:8081

Тестовые пользователи Keycloak (пароль `password`):

| Логин | ФИО |
|---|---|
| `ivanov` | Иванов Иван |
| `petrov` | Петров Пётр |
| `sidorov` | Сидоров Сидор |

Остановить приложение:

```bash
docker compose down
```

Остановить и удалить данные PostgreSQL и Kafka:

```bash
docker compose down -v
```

## Локальный запуск

Если сервисы нужно запустить не в контейнерах:

```bash
docker compose up -d postgres keycloak kafka kafka-init
./mvnw clean package
cp .env.example .env
./scripts/run-local.sh
```

Скрипт запускает все JAR и останавливает их по `Ctrl+C`. Для локального Kafka
используется `localhost:9092`, для PostgreSQL — `localhost:5432`.

## Kafka и уведомления

- Topic: `bank.notifications`, consumer group: `bank-notifications`.
- В Compose topic создаёт контейнер `kafka-init`, в Helm — Job-хук после установки.
- Локально и в Helm используется один брокер Kafka в режиме KRaft.
- В Docker Compose Kafka доступна как `kafka:9092` внутри сети и `localhost:9092` с хоста.
- Accounts, Cash и Transfer сохраняют уведомление в outbox вместе с бизнес-операцией,
  затем `OutboxPublisher` публикует JSON в Kafka.
- Сообщение содержит `eventId`, `recipient`, `type`, `message`; ключ сообщения — `eventId`.
- Notifications дедуплицирует события по `eventId`, коммитит оффсет после записи
  и при ошибке повторяет обработку.
- Отдельного REST API и маршрута Gateway для Notifications нет.

## OAuth 2.0

Realm: `bank`.

| Клиент | Flow | Scopes |
|---|---|---|
| `front` | Authorization Code | `accounts.read`, `accounts.write`, `cash.write`, `transfer.write` |
| `cash-service` | Client Credentials | `service`, `accounts.read`, `accounts.write` |
| `transfer-service` | Client Credentials | `service`, `accounts.read`, `accounts.transfer` |

- JWT пользователя содержит `preferred_username` и scopes для Front.
- Внутренние `/internal/**` методы Accounts требуют `SCOPE_service` и нужный scope.
- Gateway проверяет JWT и пробрасывает `Authorization: Bearer ...`.
- Accounts не использует Client Credentials для Notifications.

## REST API

| Сервис | Метод | Путь | Доступ |
|---|---|---|---|
| Accounts | GET | `/accounts/me` | пользователь, `accounts.read` |
| Accounts | PUT | `/accounts/me` | пользователь, `accounts.write` |
| Accounts | GET | `/accounts/others` | пользователь, `accounts.read` |
| Accounts | GET | `/internal/accounts/{login}` | сервис, `service` |
| Accounts | POST | `/internal/accounts/{login}/deposit` | сервис, `accounts.write` |
| Accounts | POST | `/internal/accounts/{login}/withdraw` | сервис, `accounts.write` |
| Accounts | POST | `/internal/accounts/transfer` | сервис, `accounts.transfer` |
| Cash | POST | `/cash/deposit` | пользователь, `cash.write` |
| Cash | POST | `/cash/withdraw` | пользователь, `cash.write` |
| Transfer | POST | `/transfer` | пользователь, `transfer.write` |

Для Cash и Transfer обязателен заголовок `Idempotency-Key: <UUID>`.
Front обращается к сервисам через Gateway по путям `/api/accounts/**`,
`/api/cash/**` и `/api/transfer/**`.

## Денежные операции

- Суммы проверяются на положительное значение и не более двух знаков после запятой.
- Баланс не может стать отрицательным.
- Accounts блокирует счёт через `SELECT ... FOR UPDATE`; перевод блокирует оба счёта
  в детерминированном порядке.
- Повторный запрос с тем же `request_id` возвращает сохранённый результат.
- Cash и Transfer фиксируют операцию как `PENDING`, затем `SUCCESS` или `FAILED`;
  зависшие операции повторяются планировщиком.
- Outbox использует `FOR UPDATE SKIP LOCKED` и переводит событие в `SENT`
  только после подтверждения Kafka.

## Тесты

```bash
./mvnw clean verify
```

Проверяются:

- unit- и MVC-тесты сервисов и контроллеров;
- интеграционные тесты на H2 в режиме PostgreSQL;
- конкурентные операции Accounts на Testcontainers PostgreSQL;
- Kafka-тесты через `@EmbeddedKafka`: публикация outbox, ретраи, дедупликация
  и продолжение обработки после перезапуска consumer’а;
- контрактные тесты Accounts через Spring Cloud Contract;
- Gateway fallback и Front MVC/Thymeleaf.

Для тестов Accounts с Testcontainers нужен Docker. Без Docker они пропускаются.

## База данных

| Сервис | БД | Пользователь |
|---|---|---|
| accounts | `accounts_db` | `accounts_user` |
| cash | `cash_db` | `cash_user` |
| transfer | `transfer_db` | `transfer_user` |
| notifications | `notifications_db` | `notifications_user` |

Миграции выполняет Flyway. Начальные аккаунты создаются миграцией
`V2__seed_accounts.sql`.

## Конфигурация

Общие настройки сервисов находятся в `config-server/src/main/resources/config-repo/`.
Переменные окружения и локальные `application.yml` задают значения по умолчанию.
Для production секреты нужно передавать через переменные окружения или Kubernetes Secret.

## Kubernetes (Helm)

Чарт `helm/bank-app/` содержит сабчарты всех сервисов и Kafka:

```text
helm/bank-app/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-dev-docker.yaml
├── values-test.yaml
├── values-prod.yaml
└── charts/
    ├── discovery-server/  config-server/  gateway/  front/
    ├── accounts/  cash/  transfer/  notifications/
    └── kafka/
```

Kafka разворачивается как StatefulSet с PVC, одним брокером KRaft и Job-хуком,
который создаёт topic `bank.notifications`. PostgreSQL и Keycloak в чарт не входят:
для них используйте Docker Compose или отдельные сервисы Kubernetes. Допускается
только один брокер: `kafka.replicas` и `kafka.topic.replicationFactor` должны быть
равны `1`.

| Окружение | Namespace | Values | Kafka PVC | Партиции |
|---|---|---|---:|---:|
| dev | `bank-dev` | `values-dev.yaml` | 2Gi | 3 |
| test | `bank-test` | `values-test.yaml` | 2Gi | 3 |
| prod | `bank-prod` | `values-prod.yaml` | 10Gi | 6 |

Установка:

```bash
kubectl create namespace bank-dev
helm upgrade --install bank ./helm/bank-app \
  -n bank-dev \
  -f helm/bank-app/values-dev.yaml
```

Если PostgreSQL и Keycloak запущены в Docker Compose, используйте overlay:

```bash
helm upgrade --install bank ./helm/bank-app \
  -n bank-dev \
  -f helm/bank-app/values-dev.yaml \
  -f helm/bank-app/values-dev-docker.yaml
```

Для Docker Desktop подходит `host.docker.internal`, для minikube —
`host.minikube.internal`, для kind нужно указать IP хоста в overlay.

Kafka использует PVC, поэтому в кластере должен быть StorageClass по умолчанию:

```bash
kubectl get storageclass
```

Если его нет, установите provisioner или задайте класс явно:

```bash
helm upgrade --install bank ./helm/bank-app -n bank-dev \
  -f helm/bank-app/values-dev.yaml \
  --set kafka.persistence.storageClass=local-path
```

Kafka запускается с `fsGroup: 1000`, чтобы могла писать в PVC.

Проверка чарта и удаление:

```bash
helm lint ./helm/bank-app
helm template bank ./helm/bank-app -n bank-dev -f helm/bank-app/values-dev.yaml
helm uninstall bank -n bank-dev
```
