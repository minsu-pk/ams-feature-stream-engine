package com.kbank.ams.featurestreamengine.adapter.out.persistence;

import com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template.GlkJdbcTemplate;
import com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template.MultiDbJdbcOperations;
import com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template.SgsJdbcTemplate;
import com.kbank.ams.featurestreamengine.application.port.out.LoadJdbcPort;
import com.kbank.ams.featurestreamengine.common.annotations.PersistenceAdapter;
import com.kbank.ams.featurestreamengine.domain.command.LoadOneJdbcCommand;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@PersistenceAdapter
@Slf4j
@RequiredArgsConstructor
public class JdbcQueryAdapter implements LoadJdbcPort {
    private final GlkJdbcTemplate glkJdbcTemplate;
    private final SgsJdbcTemplate sgsJdbcTemplate;

    @Override
    public Map<String, Object> loadOne(LoadOneJdbcCommand command) {
        return JdbcOperation(command.getDatabase()).selectOneWithParams(command.getQuery(), command.getFieldSpecs(), command.getParams());
    }

    @Override
    public Boolean loadBool(LoadOneJdbcCommand command) {
        return JdbcOperation(command.getDatabase()).selectBool(command.getQuery(), command.getParams());
    }

    private MultiDbJdbcOperations JdbcOperation(FlowEnum.Database database){
        switch (database) {
            case GOLDILOCKS -> {return glkJdbcTemplate;}
            default -> {return sgsJdbcTemplate;}
        }
    }
}
