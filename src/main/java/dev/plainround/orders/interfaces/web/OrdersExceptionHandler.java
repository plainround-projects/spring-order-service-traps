package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.InvalidCustomer;
import dev.plainround.orders.domain.InvalidOrder;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one place errors become {@code application/problem+json}: the service's named errors,
 * validation failures with the failing fields, the framework's own exceptions (through the parent
 * class) and a last resort that never leaks the cause.
 */
@RestControllerAdvice
public class OrdersExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OrdersExceptionHandler.class);
    private static final String PROBLEM_URN = "urn:plainround:problem:";

    /**
     * Maps an unknown customer to 404.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(CustomerNotFound.class)
    public ProblemDetail handleCustomerNotFound(CustomerNotFound exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "customer-not-found",
                "Customer not found",
                exception.getMessage());
    }

    /**
     * Maps a stock shortfall to 409; the checkout was rolled back as a whole.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(OutOfStock.class)
    public ProblemDetail handleOutOfStock(OutOfStock exception) {
        return problem(HttpStatus.CONFLICT, "out-of-stock", "Out of stock", exception.getMessage());
    }

    /**
     * Maps a lost stock race to 409; retrying the checkout is safe because nothing was kept.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(StockConflict.class)
    public ProblemDetail handleStockConflict(StockConflict exception) {
        return problem(
                HttpStatus.CONFLICT,
                "concurrent-update",
                "Concurrent update",
                exception.getMessage());
    }

    /**
     * Maps a rule break the field annotations cannot express (such as a repeated SKU) to 422.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler({InvalidOrder.class, InvalidCustomer.class})
    public ProblemDetail handleInvalid(RuntimeException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-order",
                "Invalid request",
                exception.getMessage());
    }

    /**
     * Maps anything unexpected to 500 without leaking its message.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("unexpected error", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Internal error",
                "The request failed.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(OrdersExceptionHandler::fieldError)
                        .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors =
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> errorsOf(result).stream())
                        .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    private static List<Map<String, String>> errorsOf(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            return parameterErrors.getFieldErrors().stream()
                    .map(OrdersExceptionHandler::fieldError)
                    .toList();
        }
        String parameter = String.valueOf(result.getMethodParameter().getParameterName());
        return result.getResolvableErrors().stream()
                .map(
                        error ->
                                Map.of(
                                        "field",
                                        parameter,
                                        "message",
                                        String.valueOf(error.getDefaultMessage())))
                .toList();
    }

    private static Map<String, String> fieldError(FieldError error) {
        return Map.of(
                "field", error.getField(), "message", String.valueOf(error.getDefaultMessage()));
    }

    private static ProblemDetail validationProblem(List<Map<String, String>> errors) {
        ProblemDetail problem =
                problem(
                        HttpStatus.BAD_REQUEST,
                        "validation-failed",
                        "Validation failed",
                        "The request failed validation.");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(
            HttpStatus status, String name, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_URN + name));
        problem.setTitle(title);
        return problem;
    }
}
