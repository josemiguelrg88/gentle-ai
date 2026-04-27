import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DisplayName("TICKET-123 - Order integration flows")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("app_test")
            .withUsername("test")
            .withPassword("test");

    // Define min/max memory in the container runtime used by the project.
    // Example decision record: PostgreSQL min=256m max=512m for repository-backed order flows.

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Nested
    @DisplayName("TICKET-123 - Create order persistence")
    class CreateOrder {

        @BeforeEach
        void cleanData() {
            orderRepository.deleteAll();
        }

        @Test
        @DisplayName("TICKET-123 - persists order without leaking data between runs")
        void persistsOrderWithoutLeakingDataBetweenRuns() {
            var order = new Order("customer-1");

            orderRepository.save(order);

            assertThat(orderRepository.findAll())
                    .singleElement()
                    .extracting(Order::getCustomerId)
                    .isEqualTo("customer-1");
        }
    }
}
