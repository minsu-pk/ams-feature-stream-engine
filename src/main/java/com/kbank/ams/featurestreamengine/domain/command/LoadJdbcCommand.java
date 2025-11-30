package com.kbank.ams.featurestreamengine.domain.command;

import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
public final class LoadJdbcCommand implements LoadCommand {
    private FlowEnum.Database database;
    private String query;
    private List<FlowModel.FieldSpec> fieldSpecs;

    @Builder
    public LoadJdbcCommand(FlowEnum.Database database, String query, List<FlowModel.FieldSpec> fieldSpecs) {
        this.database = database;
        this.query = query;
        this.fieldSpecs = fieldSpecs;
    }
}
