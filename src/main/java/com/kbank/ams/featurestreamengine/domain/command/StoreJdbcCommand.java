package com.kbank.ams.featurestreamengine.domain.command;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public final class StoreJdbcCommand<T> implements StoreCommand {
    private String updateSql;
    private List<T> items;

    @Builder
    public StoreJdbcCommand(String updateSql, List<T> items) {
        this.updateSql = updateSql;
        this.items = items;
    }
}
