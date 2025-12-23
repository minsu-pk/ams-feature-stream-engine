package com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template;

import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public final class SgsJdbcTemplate<T> implements MultiDbJdbcOperations<T> {
    @Qualifier("singlestoreJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    @Qualifier("singlestoreNamedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Map<String, Object>> select(String query, List<FlowModel.FieldSpec> fieldSpecs) {
        return jdbcTemplate.query(query, JdbcTemplateFieldMapper.rowMapper(fieldSpecs));
    }

    public List<Map<String, Object>> selectWithParams(String query, List<FlowModel.FieldSpec> fieldSpecs, Map<String, Object> params){
        return namedParameterJdbcTemplate.query(query, params, JdbcTemplateFieldMapper.rowMapper(fieldSpecs));
    }

    @Override
    public Map<String, Object> selectOneWithParams(String query, List<FlowModel.FieldSpec> fieldSpecs, Map<String, Object> params) {
        return namedParameterJdbcTemplate.queryForObject(query, params, JdbcTemplateFieldMapper.rowMapper(fieldSpecs));
    }

    @Override
    public Boolean selectBool(String query, Map<String, Object> params) {
        try {
            return namedParameterJdbcTemplate.queryForObject(query, params, Boolean.class);
        } catch (DataAccessException e) {
            log.info("{}", e);
            return true;
        }
    }

    @Override
    public int bulkInsert(String updateSql, List<T> items) {
        if (items == null || items.isEmpty()) return 0;
        SqlParameterSource[] batch = items.stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);

        int[] result = namedParameterJdbcTemplate.batchUpdate(updateSql, batch);

        return Arrays.stream(result).filter(v-> v > 0).sum();
    }
}
