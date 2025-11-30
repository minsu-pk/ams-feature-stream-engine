package com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template;

import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;
import java.util.List;
import java.util.Map;

public sealed interface MultiDbJdbcOperations permits SgsJdbcTemplate, GlkJdbcTemplate{
    List<Map<String, Object>> select(String query, List<FlowModel.FieldSpec> fieldSpecs);
    List<Map<String, Object>> selectWithParams(String query, List<FlowModel.FieldSpec> fieldSpecs, Map<String, Object> params);
    Map<String, Object> selectOneWithParams(String query, List<FlowModel.FieldSpec> fieldSpecs, Map<String, Object> params);
    Boolean selectBool(String query, Map<String, Object> params);
}
