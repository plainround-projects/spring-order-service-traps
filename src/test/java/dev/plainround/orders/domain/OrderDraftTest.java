package dev.plainround.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OrderDraftTest {

    @Test
    void aDraftNeedsAtLeastOneLine() {
        assertThatThrownBy(() -> new OrderDraft(1, List.of()))
                .isInstanceOf(InvalidOrder.class)
                .hasMessageContaining("1 to 50 lines");
    }

    @Test
    void aDraftHasAtMostFiftyLines() {
        List<OrderLine> fiftyOne =
                IntStream.rangeClosed(1, 51)
                        .mapToObj(index -> new OrderLine("sku-" + index, 1))
                        .toList();

        assertThatThrownBy(() -> new OrderDraft(1, fiftyOne)).isInstanceOf(InvalidOrder.class);
    }

    @Test
    void theSameSkuOnTwoLinesIsRejected() {
        assertThatThrownBy(
                        () ->
                                new OrderDraft(
                                        1,
                                        List.of(
                                                new OrderLine("sku-1", 1),
                                                new OrderLine("sku-1", 2))))
                .isInstanceOf(InvalidOrder.class)
                .hasMessageContaining("sku");
    }

    @Test
    void theDraftKeepsItsOwnCopyOfTheLines() {
        List<OrderLine> lines = new ArrayList<>(List.of(new OrderLine("sku-1", 1)));
        OrderDraft draft = new OrderDraft(1, lines);

        lines.add(new OrderLine("sku-2", 1));

        assertThat(draft.lines()).hasSize(1);
    }
}
