package dev.plainround.orders.interfaces.web;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.plainround.orders.application.CheckoutService;
import dev.plainround.orders.application.OrderQueryService;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.InvalidOrder;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The web layer only: the controller, its advice and the MVC machinery; the services are test
 * doubles.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String CHECKOUT = "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}";
    private static final Order STORED =
            new Order(
                    10,
                    1,
                    Instant.parse("2026-10-27T10:00:00Z"),
                    List.of(new OrderLine("sku-1", 2)));

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CheckoutService checkout;

    @MockitoBean private OrderQueryService queries;

    @Test
    void checkoutReturns201WithTheStoredOrder() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenReturn(STORED);

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.lines[0].sku").value("sku-1"));
    }

    @Test
    void anEmptyBasketIs400NamingTheLines() throws Exception {
        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[0].field").value("lines"));
    }

    @Test
    void aBadLineIs400NamingTheLineAndField() throws Exception {
        String body = "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":0}]}";

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("lines[0].quantity"));
    }

    @Test
    void anUnknownCustomerIs404() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenThrow(new CustomerNotFound(1));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"))
                .andExpect(jsonPath("$.detail").value("no customer with id 1"));
    }

    @Test
    void anOutOfStockLineIs409() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenThrow(new OutOfStock("sku-1"));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Out of stock"));
    }

    @Test
    void aLostStockRaceIs409NamedConcurrentUpdate() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenThrow(new StockConflict("sku-1"));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent update"));
    }

    @Test
    void aRuleTheAnnotationsCannotExpressIs422() throws Exception {
        when(checkout.checkout(anyLong(), anyList()))
                .thenThrow(new InvalidOrder("each sku may appear on one line only"));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("each sku may appear on one line only"));
    }

    @Test
    void listReturnsTheOrdersWithTheirLines() throws Exception {
        when(queries.listOrders(1)).thenReturn(List.of(STORED));

        mockMvc.perform(get("/customers/1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lines[0].quantity").value(2));
    }

    @Test
    void aNonNumericCustomerIdIsAProblemDetail400() throws Exception {
        mockMvc.perform(get("/customers/abc/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void anUnexpectedErrorIs500WithoutLeakingTheMessage() throws Exception {
        when(queries.listOrders(1))
                .thenThrow(new IllegalStateException("jdbc:postgresql://secret-host/orders"));

        mockMvc.perform(get("/customers/1/orders"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal error"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "secret-host"))));
    }
}
