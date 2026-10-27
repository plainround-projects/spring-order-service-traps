package dev.plainround.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.InvalidOrder;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CheckoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");
    private static final List<OrderLine> LINES =
            List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1));

    private final CustomerService customers = mock(CustomerService.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final StockRepository stock = mock(StockRepository.class);
    private final AuditLog audit = mock(AuditLog.class);
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service =
                new CheckoutService(
                        customers, orders, stock, audit, Clock.fixed(NOW, ZoneOffset.UTC));
        when(customers.require(1)).thenReturn(new Customer(1, "Ada"));
        when(orders.place(any(OrderDraft.class), any(Instant.class)))
                .thenReturn(new Order(10, 1, NOW, LINES));
    }

    @Test
    void checkoutAuditsFirstThenPlacesTheOrderThenReservesEveryLine() {
        Order placed = service.checkout(1, LINES);

        assertThat(placed.id()).isEqualTo(10);
        InOrder inOrder = inOrder(audit, customers, orders, stock);
        inOrder.verify(audit).record("CHECKOUT_ATTEMPT", "customer 1, 2 line(s)");
        inOrder.verify(customers).require(1);
        inOrder.verify(orders).place(new OrderDraft(1, LINES), NOW);
        inOrder.verify(stock).reserve("sku-1", 2);
        inOrder.verify(stock).reserve("sku-2", 1);
    }

    @Test
    void anOutOfStockLineStopsTheCheckoutAndLaterLinesAreNotReserved() {
        doThrow(new OutOfStock("sku-1")).when(stock).reserve("sku-1", 2);

        assertThatThrownBy(() -> service.checkout(1, LINES)).isInstanceOf(OutOfStock.class);

        verify(stock, never()).reserve("sku-2", 1);
    }

    @Test
    void anUnknownCustomerStillLeavesTheAuditEntryAndPlacesNothing() {
        when(customers.require(9)).thenThrow(new CustomerNotFound(9));

        assertThatThrownBy(() -> service.checkout(9, LINES)).isInstanceOf(CustomerNotFound.class);

        verify(audit).record("CHECKOUT_ATTEMPT", "customer 9, 2 line(s)");
        verify(orders, never()).place(any(), any());
        verify(stock, never()).reserve(anyString(), anyInt());
    }

    @Test
    void aRepeatedSkuIsRejectedBeforeAnythingIsStored() {
        List<OrderLine> repeated = List.of(new OrderLine("sku-1", 1), new OrderLine("sku-1", 2));

        assertThatThrownBy(() -> service.checkout(1, repeated)).isInstanceOf(InvalidOrder.class);

        verify(orders, never()).place(any(), any());
    }
}
