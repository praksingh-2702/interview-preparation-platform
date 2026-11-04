package com.prepmind.prepapi.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v3: roles live in users.role now. The old user_roles table (client-chosen roles, never trusted)
 * is dropped so it can't block deleting accounts. Harmless to run again: IF EXISTS.
 */
@Component
@Order(0)
public class LegacySchemaCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaCleanup.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            entityManager.createNativeQuery("DROP TABLE IF EXISTS user_roles").executeUpdate();
        } catch (RuntimeException e) {
            log.warn("Could not drop legacy table user_roles: {}", e.getMessage());
        }
    }
}
