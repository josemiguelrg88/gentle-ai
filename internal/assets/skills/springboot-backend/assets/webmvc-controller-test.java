import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("TICKET-123 - Order controller API")
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Nested
    @DisplayName("TICKET-123 - Create order endpoint")
    class CreateOrder {

        @Test
        @DisplayName("TICKET-123 - returns created order when request is valid")
        void returnsCreatedOrderWhenRequestIsValid() throws Exception {
            var response = new OrderResponse("order-1", "customer-1");

            given(orderService.createOrder(any(CreateOrderRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "customerId": "customer-1",
                                      "items": ["sku-1"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("order-1"))
                    .andExpect(jsonPath("$.customerId").value("customer-1"));
        }
    }
}
