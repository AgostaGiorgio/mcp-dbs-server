package com.mcp.dbs;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties
public class DbsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbsApplication.class, args);
	}

	@Bean
	public List<ToolCallback> dbTools(List<DBTool> tools) {
		return tools.stream()
				.flatMap(tool -> tool.getTools().stream())
				.collect(Collectors.toList());
	}
}
