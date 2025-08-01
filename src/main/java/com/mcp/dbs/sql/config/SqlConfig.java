package com.mcp.dbs.sql.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import io.r2dbc.spi.ConnectionFactory;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Setter
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.r2dbc.url")
public class SqlConfig {

    @Value("${spring.r2dbc.url}")
    private String dbUrl;
    
    @NonNull
    private final ConnectionFactory connectionFactory;


    @PostConstruct
    private void init() {
         Mono.from(connectionFactory.create())
            .flatMap(connection ->
                Flux.from(connection.createStatement("SELECT 1").execute())
                    .flatMap(result -> result.map((row, meta) -> row.get(0, Integer.class)))
                    .single()
                    .doFinally(signal -> connection.close())
            )
            .doOnNext(result -> {
                if (result == 1) {
                    log.info("✅ Database connection verified via SELECT 1.");
                } else {
                    throw new IllegalStateException("❌ Unexpected result from SELECT 1: " + result);
                }
            })
            .doOnError(error -> {
                throw new RuntimeException("❌ Error verifying database connection: " + error.getMessage(), error);
            })
            .block();
    }
}
