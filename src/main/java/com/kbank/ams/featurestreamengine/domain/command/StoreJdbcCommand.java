package com.kbank.ams.featurestreamengine.domain.command;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public final class StoreJdbcCommand<T> implements StoreCommand {
    private String updatedSql;
    private List<T> items;

    @Builder
    public StoreJdbcCommand(String updatedSql, List<T> items) {
        this.updatedSql = updatedSql;
        this.items = items;
    }
}
