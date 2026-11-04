package com.prepmind.prepapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Injected instead of calling Instant.now() directly, so time-based logic is testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
