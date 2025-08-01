package com.mcp.dbs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Configuration
@RequiredArgsConstructor
public class ClientConfig {
    
    @Getter
    @Value("${client.read.enabled:true}")
    private boolean readMode;

    @Getter
    @Value("${client.write.enabled:false}")
    private boolean writeMode;
}
