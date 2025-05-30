package com.mcp.dbs.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.driver.Value;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.stereotype.Service;

import com.mcp.dbs.config.Neo4jConfig;
import com.mcp.dbs.converter.ReactorConverter;
import com.mcp.dbs.pojo.Neo4jSchema;
import com.mcp.dbs.utils.Neo4jConstants;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jToolImpl implements DBTool {

    @NonNull
    private final ReactiveNeo4jClient client;

    @NonNull
    private final Neo4jConfig neo4jConfig;

    @Override
    public List<ToolCallback> getTools() {
        log.debug("Loading Neo4j Tools...");
        List<ToolCallback> tools = Arrays.stream(ToolCallbacks.from(this))
                .collect(Collectors.toList());

        if (!neo4jConfig.isReadMode()) {
            log.debug("Read mode is disabled, filtering out read tools");
            tools.removeIf(tool -> tool.getToolDefinition().name().equalsIgnoreCase("Execute Neo4j read query"));
        }

        if (!neo4jConfig.isWriteMode()) {
            log.debug("Write mode is disabled, filtering out write tools");
            tools.removeIf(tool -> tool.getToolDefinition().name().equalsIgnoreCase("Execute Neo4j write query"));
        }

        log.info("Loaded {} Neo4j tools", tools.size());
        return tools;
    }

    @Tool(name = "Get Neo4j schema", description = """
            Get the structure of the Neo4j database schema, including nodes and edges.
            Returns a list of nodes with their labels and properties, and edges with their names, properties, and source/target node labels.
            """, resultConverter = ReactorConverter.class)
    private Mono<Neo4jSchema> getSchemaStructure() {
        log.info("Fetching Neo4j schema structure...");
        return Mono.zip(getNodes(), getEdges(), Neo4jSchema::new);
    }

    @Tool(name = "Execute Neo4j read query", description = "Executes a read query on the Neo4j database and returns the result as a map.", resultConverter = ReactorConverter.class)
    private Flux<Map<String, Object>> execReadQuery(
            @ToolParam(description = "The Cypher query to execute") String query) {
        log.info("Executing read query: {}", query);
        if (!neo4jConfig.isReadMode()) {
            throw new IllegalStateException("Read mode is not enabled in the configuration.");
        }
        if (isWriteQuery(query)) {
            throw new IllegalArgumentException(
                    "The provided query is a write query, but this method only supports read queries.");
        }
        return client.query(query)
                .fetch()
                .all();
    }

    @Tool(name = "Execute Neo4j write query", description = "Executes a write query on the Neo4j database and returns the result as a map.", resultConverter = ReactorConverter.class)
    private Flux<Map<String, Object>> execWriteQuery(
            @ToolParam(description = "The Cypher query to execute") String query) {
        log.info("Executing write query: {}", query);
        if (!neo4jConfig.isWriteMode()) {
            throw new IllegalStateException("Write mode is not enabled in the configuration.");
        }
        if (isReadQuery(query) && !neo4jConfig.isReadMode()) {
            throw new IllegalStateException("Read mode is not enabled in the configuration.");
        }
        return client.query(query)
                .fetch()
                .all();
    }

    private boolean isWriteQuery(String cypherQuery) {
        if (cypherQuery == null)
            return false;

        String regex = "(?i)\\b(CREATE|MERGE|SET|DELETE|REMOVE|DROP|LOAD CSV|CALL.*apoc\\..*create)\\b";
        return cypherQuery.matches("(?s).*" + regex + ".*");
    }

    private boolean isReadQuery(String cypherQuery) {
        return !isWriteQuery(cypherQuery);
    }

    private Mono<List<Neo4jSchema.Neo4jNode>> getNodes() {

        return client.query("CALL db.labels()")
                .fetchAs(String.class)
                .all()
                .flatMap(label -> {
                    String query = String.format("MATCH (n:`%s`) RETURN DISTINCT keys(n) AS props", label);
                    return client
                            .query(query)
                            .fetchAs(Neo4jSchema.Neo4jNode.class)
                            .mappedBy((it, record) -> {
                                List<String> properties = record.get("props").asList(Value::asString);
                                return new Neo4jSchema.Neo4jNode(label, properties);
                            })
                            .all();
                })
                .collectList();
    }

    private Mono<List<Neo4jSchema.Neo4jEdge>> getEdges() {

        return client
                .query("MATCH (a)-[r]->(b) " +
                        "WITH type(r) as relType, keys(r) as relProp, labels(a)[0] as sourceLabel, labels(b)[0] as targetLabel "
                        +
                        "RETURN DISTINCT relType, relProp, sourceLabel, targetLabel")
                .fetchAs(Neo4jSchema.Neo4jEdge.class)
                .mappedBy((it, record) -> {
                    String name = record.get("relType").asString();
                    List<String> properties = record.get("relProp").asList(Value::asString);
                    String sourceLabel = record.get("sourceLabel").asString();
                    String targetLabel = record.get("targetLabel").asString();
                    return new Neo4jSchema.Neo4jEdge(name, properties, sourceLabel, targetLabel);
                })
                .all()
                .collectList();
    }
}
