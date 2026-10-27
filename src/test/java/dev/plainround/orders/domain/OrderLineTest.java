package dev.plainround.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderLineTest {

    @Test
    void aLineKeepsItsSkuAndQuantity() {
        OrderLine line = new OrderLine("sku-1", 3);

        assertThat(line.sku()).isEqualTo("sku-1");
        assertThat(line.quantity()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1001})
    void quantitiesOutsideOneToAThousandAreRejected(int quantity) {
        assertThatThrownBy(() -> new OrderLine("sku-1", quantity))
                .isInstanceOf(InvalidOrder.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void blankOrOverlongSkusAreRejected() {
        assertThatThrownBy(() -> new OrderLine(" ", 1)).isInstanceOf(InvalidOrder.class);
        assertThatThrownBy(() -> new OrderLine(null, 1)).isInstanceOf(InvalidOrder.class);
        assertThatThrownBy(() -> new OrderLine("x".repeat(65), 1)).isInstanceOf(InvalidOrder.class);
    }
}
