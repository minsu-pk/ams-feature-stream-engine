package com.kbank.ams.featurestreamengine.adapter.out.persistence;

import com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template.GlkJdbcTemplate;
import com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template.MultiDbJdbcOperations;
import com.kbank.ams.featurestreamengine.adapter.out.persistence.jdbc.template.SgsJdbcTemplate;
import com.kbank.ams.featurestreamengine.application.port.out.StoreJdbcPort;
import com.kbank.ams.featurestreamengine.common.annotations.PersistenceAdapter;
import com.kbank.ams.featurestreamengine.domain.command.StoreJdbcCommand;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@PersistenceAdapter
@Slf4j
@RequiredArgsConstructor
public class JdbcStoreAdapter implements StoreJdbcPort {
    private final GlkJdbcTemplate glkJdbcTemplate;
    private final SgsJdbcTemplate sgsJdbcTemplate;

    @Override
    public int store(StoreJdbcCommand command) {
        return JdbcOperation(FlowEnum.Database.SINGLESTORE).bulkInsert(command.getUpdatedSql(), command.getItems());
    }

    private MultiDbJdbcOperations JdbcOperation(FlowEnum.Database database){
        switch (database) {
            case GOLDILOCKS -> {return glkJdbcTemplate;}
            default -> {return sgsJdbcTemplate;}
        }
    }
}
