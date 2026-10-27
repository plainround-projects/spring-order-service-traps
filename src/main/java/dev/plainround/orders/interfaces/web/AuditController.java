package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.AuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of the audit trail (read only). */
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService service;

    /**
     * Wires the controller.
     *
     * @param service the audit queries
     */
    public AuditController(AuditService service) {
        this.service = service;
    }

    /**
     * Returns the newest audit entries first.
     *
     * @param limit how many at most, 1 to 100
     * @return the entries
     */
    @GetMapping
    public List<AuditResponse> latest(
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "limit must be at least 1")
                    @Max(value = 100, message = "limit must be at most 100")
                    int limit) {
        return service.latest(limit).stream().map(AuditResponse::from).toList();
    }
}
