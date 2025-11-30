package com.kbank.ams.featurestreamengine.domain.flow;

public class FlowEnum {
    public enum Database {
        SINGLESTORE,
        GOLDILOCKS,
    }

    public enum ProcessType {
        DERIVED,
        FILTER,
    }
    public enum ProcessDetailType {
        DB_QUERY,
        REQ_API,
        OPERATOR,
    }

    public enum FieldType {
        STRING,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        SQL_DATETIME,
        TIMESTAMP,
        JAVA_DATETIME,
    }
}
