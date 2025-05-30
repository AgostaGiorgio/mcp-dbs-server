package com.mcp.dbs.service;

import java.util.List;

import org.neo4j.driver.Value;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.stereotype.Service;

import com.mcp.dbs.converter.ReactorConverter;
import com.mcp.dbs.pojo.Neo4jSchema;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jToolsImpl implements Neo4jTools {

    @NonNull
    private final ReactiveNeo4jClient client;


    @Override
    @Tool(name = "Get Neo4j schema", description = """
            Get the structure of the Neo4j database schema, including nodes and edges.
            Returns a list of nodes with their labels and properties, and edges with their names, properties, and source/target node labels.
            """, resultConverter = ReactorConverter.class)
    public Mono<Neo4jSchema> getSchemaStructure() {
        return Mono.zip(getNodes(), getEdges(), Neo4jSchema::new);
    }


    private Mono<List<Neo4jSchema.Neo4jNode>> getNodes() {

        return client
                .query("CALL db.labels()")
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
