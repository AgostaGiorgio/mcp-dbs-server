package com.mcp.dbs.service;

import com.mcp.dbs.pojo.Neo4jSchema;

import reactor.core.publisher.Mono;

public interface Neo4jTools {

    Mono<Neo4jSchema> getSchemaStructure();

}