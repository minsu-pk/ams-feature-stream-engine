package com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template;

import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class JdbcTemplateFieldMapper {
    protected static RowMapper<Map<String,Object>> rowMapper(List<FlowModel.FieldSpec> fieldSpecs) {
        return (rs, rowNum) -> {
            Map<String,Object> item = new HashMap<>();
            for (FlowModel.FieldSpec fieldSpec : fieldSpecs) {
                item.put(fieldSpec.getName(), fieldSpec.extractValue(rs));
            }
            return item;
        };
    }

    protected static List<SqlParameterSource> toSqlParams(List<Map<String,Object>> items, List<FlowModel.FieldSpec> fieldSpecs) {
        List<SqlParameterSource> params = new ArrayList<>();
        for (Map<String,Object> item : items) {
            MapSqlParameterSource p = new MapSqlParameterSource();
            for (FlowModel.FieldSpec fieldSpec : fieldSpecs) {
                p.addValue(fieldSpec.getName(), item.get(fieldSpec.getName()), fieldSpec.getSqlType());
            }
            params.add(p);
        }
        return params;
    }
}
