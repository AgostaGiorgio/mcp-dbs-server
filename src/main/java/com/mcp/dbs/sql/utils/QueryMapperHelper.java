package com.mcp.dbs.sql.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import com.mcp.dbs.sql.pojo.MySqlSchema;


public class QueryMapperHelper {

    public static MySqlSchema mapToSchema(List<RowData> rows) {
        Map<String, List<RowData>> groupedByTable = rows.stream().collect(Collectors.groupingBy(r -> r.table));

        List<MySqlSchema.MySqlTable> tables = new ArrayList<>();

        for (var entry : groupedByTable.entrySet()) {
            String tableName = entry.getKey();
            List<RowData> tableRows = entry.getValue();

            Map<String, MySqlSchema.MySqlTable.MySqlColumn> columns = new LinkedHashMap<>();
            List<MySqlSchema.MySqlTable.MySqlConstraint> constraints = new ArrayList<>();
            List<MySqlSchema.MySqlTable.MySqlForeignKey> foreignKeys = new ArrayList<>();
            Set<String> constraintKeys = new HashSet<>();

            for (RowData row : tableRows) {
                columns.putIfAbsent(row.column, new MySqlSchema.MySqlTable.MySqlColumn(
                        row.column, row.type, row.nullable, row.defaultValue
                ));

                if (row.constraintType != null) {
                    if (row.constraintType.equals("FOREIGN KEY")) {
                        foreignKeys.add(new MySqlSchema.MySqlTable.MySqlForeignKey(
                                row.column, row.foreignTable, row.foreignColumn
                        ));
                    } else {
                        String key = row.column + "|" + row.constraintType + "|" + row.constraintName;
                        if (constraintKeys.add(key)) {
                            constraints.add(new MySqlSchema.MySqlTable.MySqlConstraint(
                                    row.column, row.constraintType, row.constraintName
                            ));
                        }
                    }
                }
            }

            tables.add(new MySqlSchema.MySqlTable(
                    tableName,
                    new ArrayList<>(columns.values()),
                    foreignKeys,
                    constraints));
        }

        return new MySqlSchema(tables);
    }

    public static Map<String, Object> rowToMap(Row row, RowMetadata meta) {
        Map<String, Object> map = new HashMap<>();
        meta.getColumnMetadatas().forEach(col -> {
            String columnName = col.getName();
            map.put(columnName, row.get(columnName));
        });
        return map;
    }

    public static record RowData(
            String table,
            String column,
            String type,
            boolean nullable,
            String defaultValue,
            String constraintType,
            String constraintName,
            String foreignTable,
            String foreignColumn) {
    }
}
