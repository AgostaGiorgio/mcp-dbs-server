package com.mcp.dbs.config;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import io.micrometer.common.lang.NonNull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.neo4j.uri")
public class Neo4jConfig {

    @NonNull
    private final Driver driver;

    @NonNull
    private final ConfigurableApplicationContext ctx; 
    
    @NonNull
    private final ReactiveNeo4jClient client;

    @Value("${spring.data.neo4j.database:neo4j}")
    private String database;

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(
            ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
        return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider);
    }

    @PostConstruct
    private void init() {
        client.query("RETURN 1")
                .fetch()
                .one()
                .doOnError(e -> {
                    log.error("Error connecting to Neo4j database: {}", database, e);
                    System.exit(SpringApplication.exit(ctx));
                })
                .doOnSuccess(i -> log.info("Successfully connected to Neo4j database: {}", database))
                .block();
    }

}
