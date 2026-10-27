package dev.plainround.orders.interfaces.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.plainround.orders.application.AuditService;
import dev.plainround.orders.application.CustomerService;
import dev.plainround.orders.application.StockService;
import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.OutOfStock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CustomerController.class, StockController.class, AuditController.class})
class CustomerStockAuditControllersTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CustomerService customers;

    @MockitoBean private StockService stock;

    @MockitoBean private AuditService audit;

    @Test
    void registerReturns201WithTheNewCustomer() throws Exception {
        when(customers.register("Ada")).thenReturn(new Customer(1, "Ada"));

        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void aBlankNameIs400NamingTheField() throws Exception {
        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void getOfAnUnknownCustomerIs404() throws Exception {
        when(customers.require(7)).thenThrow(new CustomerNotFound(7));

        mockMvc.perform(get("/customers/7")).andExpect(status().isNotFound());
    }

    @Test
    void putSetsTheStockLevel() throws Exception {
        mockMvc.perform(
                        put("/stock/sku-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"available\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(5));

        verify(stock).setAvailable("sku-1", 5);
    }

    @Test
    void aNegativeLevelIs400() throws Exception {
        mockMvc.perform(
                        put("/stock/sku-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"available\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("available"));
    }

    @Test
    void getOfASkuWithoutAStockRecordIs409OutOfStock() throws Exception {
        when(stock.available("nope")).thenThrow(new OutOfStock("nope"));

        mockMvc.perform(get("/stock/nope")).andExpect(status().isConflict());
    }

    @Test
    void auditReturnsTheNewestEntriesAndBoundsTheLimit() throws Exception {
        when(audit.latest(20))
                .thenReturn(
                        List.of(
                                new AuditEntry(
                                        1,
                                        "CHECKOUT_ATTEMPT",
                                        "customer 1",
                                        Instant.parse("2026-10-27T10:00:00Z"))));

        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CHECKOUT_ATTEMPT"));
        mockMvc.perform(get("/audit").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }
}
