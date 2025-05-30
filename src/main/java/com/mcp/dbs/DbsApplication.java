package com.mcp.dbs;

import java.util.List;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.mcp.dbs.service.Neo4jTools;

@SpringBootApplication
@EnableConfigurationProperties
public class DbsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbsApplication.class, args);
	}

	@Bean
	public List<ToolCallback> myTools(Neo4jTools neo4jTools) {
		return List.of(ToolCallbacks.from(neo4jTools));
	}
}
