package com.mcp.dbs.utils;

import java.util.List;

public class Neo4jConstants {
    public static final List<String> WRITE_OPERATORS = List.of(
        "CreateNode",
        "CreateRelationship",
        "Merge",
        "SetProperty",
        "DeleteEntity",
        "RemoveProperty",
        "RemoveLabels",
        "Drop"
    );
}
