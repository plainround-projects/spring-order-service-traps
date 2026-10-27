package dev.plainround.orders.domain;

import java.util.HashSet;
import java.util.List;

/**
 * What a customer asks to buy, before it is stored.
 *
 * @param customerId the buyer
 * @param lines 1 to {@value #MAX_LINES} lines with distinct SKUs
 */
public record OrderDraft(long customerId, List<OrderLine> lines) {

    /** Most lines in one order. */
    public static final int MAX_LINES = 50;

    /** Checks the invariants and keeps an immutable copy of the lines. */
    public OrderDraft {
        lines = List.copyOf(lines);
        if (lines.isEmpty() || lines.size() > MAX_LINES) {
            throw new InvalidOrder("an order needs 1 to " + MAX_LINES + " lines");
        }
        if (new HashSet<>(lines.stream().map(OrderLine::sku).toList()).size() != lines.size()) {
            throw new InvalidOrder("each sku may appear on one line only");
        }
    }
}
