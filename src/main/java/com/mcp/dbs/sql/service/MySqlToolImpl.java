package com.mcp.dbs.sql.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;

import com.mcp.dbs.DBTool;
import com.mcp.dbs.config.ClientConfig;
import com.mcp.dbs.converter.ReactorConverter;
import com.mcp.dbs.sql.pojo.MySqlSchema;
import com.mcp.dbs.sql.utils.QueryMapperHelper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.r2dbc.url")
public class MySqlToolImpl implements DBTool {

    private static final String GET_SCHEMA_QUERY = "SELECT cols.table_name, cols.column_name, cols.data_type, cols.is_nullable, cols.column_default, tc.constraint_type, kcu.constraint_name, ccu.table_name AS foreign_table, ccu.column_name AS foreign_column FROM information_schema.columns cols LEFT JOIN information_schema.key_column_usage kcu ON cols.table_name = kcu.table_name AND cols.column_name = kcu.column_name LEFT JOIN information_schema.table_constraints tc ON tc.constraint_name = kcu.constraint_name AND tc.table_name = cols.table_name LEFT JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name WHERE cols.table_schema = 'public' ORDER BY cols.table_name, cols.ordinal_position";

    @NonNull
    private final ClientConfig clientConfig;

    @NonNull
    private final DatabaseClient db;

    @Override
    public List<ToolCallback> getTools() {
        log.debug("Loading MySql Tools...");
        List<ToolCallback> tools = Arrays.stream(ToolCallbacks.from(this))
                .collect(Collectors.toList());

        if (!clientConfig.isReadMode()) {
            log.debug("Read mode is disabled, filtering out read tools");
            tools.removeIf(tool -> tool.getToolDefinition().name().equalsIgnoreCase("Execute mysql read query"));
        }

        if (!clientConfig.isWriteMode()) {
            log.debug("Write mode is disabled, filtering out write tools");
            tools.removeIf(tool -> tool.getToolDefinition().name().equalsIgnoreCase("Execute mysql write query"));
        }

        log.info("Loaded {} MySql tools", tools.size());
        return tools;
    }

    @Tool(name = "Get MySQL DB schema", description = """
            Get the structure of the MySQL database, including tables, foreing keys and constraints.
            Returns a list of table names and pther info such as columns  foreign keys and other constraints.
            """, resultConverter = ReactorConverter.class)
    private Mono<MySqlSchema> getSchemaStructure() {
        log.info("Fetching MySQL schema structure...");

        return db.sql(GET_SCHEMA_QUERY)
                .map(row -> {
                    String table = row.get("table_name", String.class);
                    String column = row.get("column_name", String.class);
                    String type = row.get("data_type", String.class);
                    boolean nullable = "YES".equalsIgnoreCase(row.get("is_nullable", String.class));
                    String defaultValue = row.get("column_default", String.class);

                    String constraintType = row.get("constraint_type", String.class);
                    String constraintName = row.get("constraint_name", String.class);
                    String foreignTable = row.get("foreign_table", String.class);
                    String foreignColumn = row.get("foreign_column", String.class);

                    return new QueryMapperHelper.RowData(table, column, type, nullable, defaultValue, constraintType,
                            constraintName, foreignTable, foreignColumn);
                })
                .all()
                .collectList()
                .map(QueryMapperHelper::mapToSchema);
    }

    @Tool(name = "Execute mysql read query", description = "Executes a read query on the MySQL database and returns the result as a map.", resultConverter = ReactorConverter.class)
    private Flux<Map<String, Object>> execReadQuery(
            @ToolParam(description = "The SQL query to execute") String query) {
        log.info("Executing read query: {}", query);
        if (!clientConfig.isReadMode()) {
            throw new IllegalStateException("Read mode is not enabled in the configuration.");
        }
        if (isWriteQuery(query)) {
            throw new IllegalArgumentException(
                    "The provided query is a write query, but this method only supports read queries.");
        }

        return db.sql(query)
                .map((row, meta) -> QueryMapperHelper.rowToMap(row, meta))
                .all();
    }

    @Tool(name = "Execute mysql write query", description = "Executes a write query on the MySQL database and returns the result as a map.", resultConverter = ReactorConverter.class)
    private Flux<Map<String, Object>> execWriteQuery(
            @ToolParam(description = "The SQL query to execute") String query) {
        log.info("Executing write query: {}", query);
        if (!clientConfig.isWriteMode()) {
            throw new IllegalStateException("Write mode is not enabled in the configuration.");
        }
        if (isReadQuery(query) && !clientConfig.isReadMode()) {
            throw new IllegalStateException("Read mode is not enabled in the configuration.");
        }

        String lowered = query.trim().toLowerCase();
        boolean isInsert = lowered.startsWith("insert");
        boolean isUpdate = lowered.startsWith("update");
        boolean isDelete = lowered.startsWith("delete");
        boolean isReturningCandidate = (isInsert || isUpdate || isDelete);

        if (isReturningCandidate && !lowered.contains("returning")) {
            if (query.endsWith(";")) {
                query = query.substring(0, query.length() - 1).trim();
            }
            query += " RETURNING *";
        }

        return db.sql(query).map((row, meta) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            meta.getColumnMetadatas().forEach(col -> {
                String name = col.getName();
                map.put(name, row.get(name));
            });
            return map;
        })
                .all()
                .switchIfEmpty(Flux.just(Map.of("message", "DDL executed successfully")));
    }

    private boolean isWriteQuery(String mysqlQuery) {
        if (mysqlQuery == null)
            return false;

        String regex = "(?i)\\b(INSERT|UPDATE|DELETE|CREATE)\\b";
        return mysqlQuery.matches("(?s).*" + regex + ".*");
    }

    private boolean isReadQuery(String mysqlQuery) {
        return !isWriteQuery(mysqlQuery);
    }

}