package com.kbank.ams.featurestreamengine.domain.flow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;

public class FlowModel {
    @Getter
    @ToString
    public static class FieldSpec {
        private final String name;
        private final FlowEnum.FieldType type;
        private int sqlType;

        public FieldSpec(Map<String, Object> spec) {
            this.name = spec.get("name").toString();
            this.type = FlowEnum.FieldType.valueOf(spec.get("type").toString());

            this.sqlType = Types.VARCHAR;
            switch (type) {
                case TIMESTAMP,SQL_DATETIME -> this.sqlType = Types.TIMESTAMP;
                case INTEGER -> this.sqlType = Types.INTEGER;
                case LONG -> this.sqlType = Types.BIGINT;
                case DOUBLE -> this.sqlType = Types.DOUBLE;
            }
        }

        @SneakyThrows
        public Object extractValue(ResultSet rs) {
            try {
                switch (type) {
                    case STRING -> {
                        String value = rs.getString(name);
                        return value != null && !value.trim().isEmpty() ? value.trim() : null;
                    }
                    case INTEGER -> {
                        return rs.getObject(name, Integer.class);
                    }
                    case LONG -> {
                        return rs.getObject(name, Long.class);
                    }
                    case FLOAT -> {
                        return rs.getObject(name, Float.class);
                    }
                    case DOUBLE -> {
                        return rs.getObject(name, Double.class);
                    }
                    case TIMESTAMP, SQL_DATETIME -> {
                        return rs.getObject(name, java.sql.Timestamp.class);
                    }
//                    case OBJECT -> {
//                        return rs.getObject(name, Object.class);
//                    }
//                    case STRING_LIST -> {
//                        String value = rs.getString(name);
//                        return Arrays.stream(value.split(listSeparator)).toList();
//                    }
//                    case LONG_LIST -> {
//                        String value = rs.getString(name);
//                        return Arrays.stream(value.split(listSeparator))
//                                .map(Long::valueOf)
//                                .toList();
//                    }
//                    case INTEGER_LIST -> {
//                        String value = rs.getString(name);
//                        return Arrays.stream(value.split(listSeparator))
//                                .map(Integer::valueOf)
//                                .toList();
//                    }
                    default -> {
                        return rs.getObject(name);
                    }
                }
            }catch (SQLException e) {
                throw new RuntimeException("Failed to extract value for field : " + name, e);
            }
        }
    }

    @Getter
    @ToString
    public static class Expr {
        private final String expr;
        private final String as;

        public Expr(String expr, String as) {
            this.expr = expr;
            this.as = as;
        }

        public Expr(Map<String, Object> spec) {
            this.expr = spec.get("expr").toString();
            this.as = spec.get("as").toString();
        }
    }
}
