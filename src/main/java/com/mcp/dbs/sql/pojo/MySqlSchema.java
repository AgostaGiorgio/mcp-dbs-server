package com.mcp.dbs.sql.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MySqlSchema {
    private List<MySqlTable> tables;

    @Getter
    @AllArgsConstructor
    public static class MySqlTable {
        private String name;
        private List<MySqlColumn> columns;
        private List<MySqlForeignKey> foreignKeys;
        private List<MySqlConstraint> constraints;

        @Getter
        @AllArgsConstructor
        public static class MySqlColumn {
            private String name;
            private String type;
            private boolean nullable;
            private String defaultValue;
        }

        @Getter
        @AllArgsConstructor
        public static class MySqlForeignKey {
            private String columnName;
            private String referencedTable;
            private String referencedColumn;
        }

        @Getter
        @AllArgsConstructor
        public static class MySqlConstraint {
            private String columnName;
            private String type;
            private String expression;
        }
    }
}
