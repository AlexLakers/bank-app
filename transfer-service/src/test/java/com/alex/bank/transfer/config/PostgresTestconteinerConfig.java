package com.alex.bank.transfer.config;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;


public class PostgresTestconteinerConfig {

    @Container
    @ServiceConnection
        static final PostgreSQLContainer<?> postgreSQLContainer =
                new PostgreSQLContainer<>("postgres:17");

    }
