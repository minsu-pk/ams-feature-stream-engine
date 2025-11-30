package com.kbank.ams.featurestreamengine.domain.flow.process.attribute;

import com.kbank.ams.featurestreamengine.common.util.ResourceFileUtil;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class ProcessDbQueryAttribute implements ProcessAttribute {
    private final FlowEnum.Database database;
    private final String queryFilePath;
    private final String query;
    private final List<FlowModel.FieldSpec> fieldSpecs;

    public ProcessDbQueryAttribute(String baseDir, Map<String,Object> spec) {
        this.database = FlowEnum.Database.valueOf(spec.get("database").toString());
        this.queryFilePath = spec.get("queryFilePath").toString();
        this.query = ResourceFileUtil.load(baseDir + this.queryFilePath);
        this.fieldSpecs = spec.get("fieldSpecs") != null ? ((List<Map<String,Object>>) spec.get("fieldSpecs")).stream().map(FlowModel.FieldSpec::new).toList() : null;
    }
}
