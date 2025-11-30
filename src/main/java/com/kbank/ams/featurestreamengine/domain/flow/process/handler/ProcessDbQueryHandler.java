package com.kbank.ams.featurestreamengine.domain.flow.process.handler;

import com.kbank.ams.featurestreamengine.application.port.out.LoadJdbcPort;
import com.kbank.ams.featurestreamengine.domain.command.LoadOneJdbcCommand;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum.ProcessType;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessAttribute;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessDbQueryAttribute;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public final class ProcessDbQueryHandler implements ProcessHandler {
    private final LoadJdbcPort loadJdbcPort;
    private final FlowEnum.ProcessType type;

    public ProcessDbQueryHandler(LoadJdbcPort loadJdbcPort, ProcessType type) {
        this.loadJdbcPort = loadJdbcPort;
        this.type = type;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> item, ProcessAttribute attribute) {
        ProcessDbQueryAttribute dbQueryAttribute = (ProcessDbQueryAttribute) attribute;
        switch (type) {
            case DERIVED -> {
                Map<String, Object> derived = loadJdbcPort.loadOne(mapToCommand(dbQueryAttribute, item));
                Map<String, Object> merged = new HashMap<>(item);
                if (derived != null && !derived.isEmpty()) {
                    merged.putAll(derived);
                }
                return merged;
            }
            case FILTER -> {
                return loadJdbcPort.loadBool(mapToCommand(dbQueryAttribute, item)) ? item : null;
            }
            default -> {
                return item;
            }
        }
    }

    private LoadOneJdbcCommand mapToCommand(ProcessDbQueryAttribute attribute, Map<String, Object> params){
        return LoadOneJdbcCommand.builder()
            .database(attribute.getDatabase())
            .query(attribute.getQuery())
            .fieldSpecs(attribute.getFieldSpecs())
            .params(params)
            .build();
    }
}
