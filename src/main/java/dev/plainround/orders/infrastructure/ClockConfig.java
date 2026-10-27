package dev.plainround.orders.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The one place the real clock is created; tests replace this bean with a fixed one. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /**
     * Provides the system clock in UTC.
     *
     * @return the clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
