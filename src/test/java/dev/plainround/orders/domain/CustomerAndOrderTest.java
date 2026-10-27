package dev.plainround.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerAndOrderTest {

    @Test
    void aCustomerNeedsANameOfOneToHundredCharacters() {
        assertThat(new Customer(1, "Ada").name()).isEqualTo("Ada");
        assertThatThrownBy(() -> new Customer(1, " ")).isInstanceOf(InvalidCustomer.class);
        assertThatThrownBy(() -> Customer.requireName(null)).isInstanceOf(InvalidCustomer.class);
        assertThatThrownBy(() -> Customer.requireName("x".repeat(101)))
                .isInstanceOf(InvalidCustomer.class);
    }

    @Test
    void aStoredOrderKeepsItsOwnCopyOfTheLines() {
        List<OrderLine> lines = new ArrayList<>(List.of(new OrderLine("sku-1", 1)));
        Order order = new Order(7, 1, Instant.parse("2026-10-27T10:00:00Z"), lines);

        lines.clear();

        assertThat(order.lines()).containsExactly(new OrderLine("sku-1", 1));
    }

    @Test
    void namedErrorsSayWhatWentWrong() {
        assertThat(new CustomerNotFound(9)).hasMessage("no customer with id 9");
        assertThat(new OutOfStock("sku-1")).hasMessage("not enough stock for sku-1");
        assertThat(new StockConflict("sku-1")).hasMessageContaining("another checkout");
    }
}
