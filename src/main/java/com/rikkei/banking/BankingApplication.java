package com.rikkei.banking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BankingApplication {

    private static final Logger log = LoggerFactory.getLogger(BankingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }

    @Bean
    ApplicationRunner verifyDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            Integer selectOne = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            String database = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
            Long accountsTable = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'accounts'",
                    Long.class);

            if (selectOne == null || selectOne != 1) {
                throw new IllegalStateException("JDBC verification failed: SELECT 1 returned " + selectOne);
            }
            if (accountsTable == null || accountsTable != 1L) {
                throw new IllegalStateException("Schema verification failed: accounts table is missing");
            }

            log.info("JDBC verification succeeded: SELECT 1 => {}; current_database() => {}",
                    selectOne, database);
            log.info("Schema verification succeeded: accounts table exists");
        };
    }
}
