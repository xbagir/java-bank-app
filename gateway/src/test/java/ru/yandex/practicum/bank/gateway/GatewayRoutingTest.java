package ru.yandex.practicum.bank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что маршруты Gateway считываются из конфигурации
 * для всех микросервисов и содержат нужные фильтры.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void shouldDefineLoadBalancedRouteForEveryService() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes).isNotNull().hasSize(4);
        assertThat(routes).extracting(RouteDefinition::getId)
                .containsExactlyInAnyOrder("accounts", "cash", "transfer", "notifications");

        assertThat(routes).allSatisfy(route -> {
            assertThat(route.getUri().toString()).startsWith("lb://");
            assertThat(route.getPredicates())
                    .anySatisfy(predicate -> assertThat(predicate.getName()).isEqualTo("Path"));
            assertThat(route.getFilters())
                    .anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("StripPrefix"));
            assertThat(route.getFilters())
                    .anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("CircuitBreaker"));
        });
    }

    @Test
    void shouldRouteAccountsRequestsToAccountsService() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes).isNotNull();
        RouteDefinition accounts = routes.stream()
                .filter(route -> "accounts".equals(route.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(accounts.getUri().toString()).isEqualTo("lb://accounts");
        assertThat(accounts.getPredicates())
                .anySatisfy(predicate -> assertThat(predicate.getArgs().values())
                        .contains("/api/accounts/**"));
    }
}
