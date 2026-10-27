package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The body of {@code POST /customers/{id}/orders}.
 *
 * @param lines what is being bought
 */
public record CheckoutRequest(
        @NotEmpty(message = "lines must not be empty")
                @Size(max = OrderDraft.MAX_LINES, message = "lines must have at most 50 entries")
                List<@Valid LineRequest> lines) {

    /**
     * Converts to domain lines.
     *
     * @return the lines
     */
    public List<OrderLine> toDomain() {
        return lines.stream().map(LineRequest::toDomain).toList();
    }
}
