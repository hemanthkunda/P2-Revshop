package com.rev.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DbMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database migration for Item-Level Order Status...");

        try {
            // 1. Check if 'status' column exists in 'order_item'
            Integer columnExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = 'ORDER_ITEM' AND column_name = 'STATUS'",
                Integer.class
            );

            if (columnExists == 0) {
                log.info("Adding 'status' column to 'order_item' table...");
                jdbcTemplate.execute("ALTER TABLE order_item ADD status VARCHAR2(50) DEFAULT 'PENDING' NOT NULL");
                log.info("Successfully added 'status' column to 'order_item'.");
            } else {
                log.info("'status' column already exists in 'order_item'.");
            }

            // 2. Check if 'status' column exists in 'orders' to drop it (optional/safe)
            Integer orderStatusExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = 'ORDERS' AND column_name = 'STATUS'",
                Integer.class
            );

            if (orderStatusExists > 0) {
                log.info("Dropping legacy 'status' column from 'orders' table...");
                jdbcTemplate.execute("ALTER TABLE orders DROP COLUMN status");
                log.info("Successfully dropped 'status' column from 'orders'.");
            }

        } catch (Exception e) {
            log.error("Error during database migration: {}", e.getMessage());
            // We don't throw exception to prevent app from failing to start if migration was partially done
        }
    }
}
