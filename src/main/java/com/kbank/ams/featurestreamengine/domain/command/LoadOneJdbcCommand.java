package com.kbank.ams.featurestreamengine.domain.command;

import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class LoadOneJdbcCommand implements LoadCommand {
    private final FlowEnum.Database database;
    private final String query;
    private final List<FlowModel.FieldSpec> fieldSpecs;
    private final Map<String,Object> params;

    @Builder
    public LoadOneJdbcCommand(FlowEnum.Database database, String query, List<FlowModel.FieldSpec> fieldSpecs, Map<String, Object> params) {
        this.database = database;
        this.query = query;
        this.fieldSpecs = fieldSpecs;
        this.params = params;
    }
}
