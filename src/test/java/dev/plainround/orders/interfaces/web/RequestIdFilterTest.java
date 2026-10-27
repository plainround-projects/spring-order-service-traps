package dev.plainround.orders.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void aSafeIncomingIdIsKeptAndPutInTheLogContextForTheRequestOnly() throws Exception {
        AtomicReference<String> insideTheChain = new AtomicReference<>();
        FilterChain chain = (request, response) -> insideTheChain.set(MDC.get("requestId"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "trace-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(insideTheChain).hasValue("trace-42");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("trace-42");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void anUnsafeIncomingIdIsReplacedSoItCannotForgeLogLines() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "evil\n{\"level\":\"ERROR\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void aMissingIdIsGenerated() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }
}
