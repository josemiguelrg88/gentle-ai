import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("TICKET-123 - Order service use cases")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("TICKET-123 - Create order")
    class CreateOrder {

        @Test
        @DisplayName("TICKET-123 - creates order when request is valid")
        void createsOrderWhenRequestIsValid() {
            var request = new CreateOrderRequest("customer-1", List.of("sku-1"));

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            var response = orderService.createOrder(request);

            assertThat(response.customerId()).isEqualTo("customer-1");
            assertThat(response.items()).hasSize(1);
        }
    }
}
