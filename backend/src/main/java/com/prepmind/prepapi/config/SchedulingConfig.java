package com.prepmind.prepapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled jobs (e.g. AccountCleanup). They only run while the server is awake. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
