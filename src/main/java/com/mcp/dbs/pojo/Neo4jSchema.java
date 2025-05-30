package com.mcp.dbs.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Neo4jSchema {
    private List<Neo4jNode> nodes;
    private List<Neo4jEdge> edges;


    @Getter
    @AllArgsConstructor
    public static class Neo4jNode {
        private String label;
        private List<String> properties;
    }

    @Getter
    @AllArgsConstructor
    public static class Neo4jEdge {
        private String name;
        private List<String> properties;
        private String sourceNodeLabel;
        private String targetNodeLabel;
    }
}
