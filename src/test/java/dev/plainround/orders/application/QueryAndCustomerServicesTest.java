package dev.plainround.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.CustomerRepository;
import dev.plainround.orders.domain.InvalidCustomer;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueryAndCustomerServicesTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerService customers = new CustomerService(customerRepository);

    @Test
    void registerStoresAValidatedName() {
        when(customerRepository.register("Ada")).thenReturn(new Customer(1, "Ada"));

        assertThat(customers.register("Ada").id()).isEqualTo(1);
    }

    @Test
    void registerRejectsABlankNameBeforeStoring() {
        assertThatThrownBy(() -> customers.register(" ")).isInstanceOf(InvalidCustomer.class);

        verify(customerRepository, never()).register(" ");
    }

    @Test
    void requireOfAnUnknownIdIsCustomerNotFound() {
        when(customerRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customers.require(5)).isInstanceOf(CustomerNotFound.class);
    }

    @Test
    void listOrdersChecksTheCustomerThenReadsTheOrders() {
        OrderRepository orders = mock(OrderRepository.class);
        when(customerRepository.findById(1)).thenReturn(Optional.of(new Customer(1, "Ada")));
        Order order =
                new Order(
                        10,
                        1,
                        Instant.parse("2026-10-27T10:00:00Z"),
                        List.of(new OrderLine("sku-1", 1)));
        when(orders.findByCustomerId(1)).thenReturn(List.of(order));

        assertThat(new OrderQueryService(customers, orders).listOrders(1)).containsExactly(order);
    }

    @Test
    void stockServiceSetsAndReadsLevelsAndNamesAMissingSku() {
        StockRepository stock = mock(StockRepository.class);
        StockService service = new StockService(stock);
        when(stock.available("sku-1")).thenReturn(Optional.of(4));
        when(stock.available("nope")).thenReturn(Optional.empty());

        service.setAvailable("sku-1", 4);

        verify(stock).setAvailable("sku-1", 4);
        assertThat(service.available("sku-1")).isEqualTo(4);
        assertThatThrownBy(() -> service.available("nope")).isInstanceOf(OutOfStock.class);
    }

    @Test
    void auditServiceReturnsTheNewestEntries() {
        AuditLog log = mock(AuditLog.class);
        AuditEntry entry =
                new AuditEntry(
                        1, "CHECKOUT_ATTEMPT", "customer 1", Instant.parse("2026-10-27T10:00:00Z"));
        when(log.latest(5)).thenReturn(List.of(entry));

        assertThat(new AuditService(log).latest(5)).containsExactly(entry);
    }
}
