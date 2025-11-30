package com.kbank.ams.featurestreamengine.domain.flow.process;

import com.kbank.ams.featurestreamengine.application.port.out.LoadJdbcPort;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessAttribute;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessDbQueryAttribute;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessOperatorAttribute;
import com.kbank.ams.featurestreamengine.domain.flow.process.handler.ProcessDbQueryHandler;
import com.kbank.ams.featurestreamengine.domain.flow.process.handler.ProcessHandler;
import com.kbank.ams.featurestreamengine.domain.flow.process.handler.ProcessOperatorHandler;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@ToString
@Slf4j
public class Process {
    private final FlowEnum.ProcessType type;
    private final FlowEnum.ProcessDetailType detailType;
    private ProcessAttribute attribute;
    private ProcessHandler handler;
    private final LoadJdbcPort loadJdbcPort;

    public Process(
        String baseDir,
        Map<String,Object> spec,
        LoadJdbcPort loadJdbcPort
    ) {
        this.type = FlowEnum.ProcessType.valueOf(spec.get("type").toString());
        this.detailType = FlowEnum.ProcessDetailType.valueOf(spec.get("detailType").toString());
        this.loadJdbcPort = loadJdbcPort;

        switch (detailType) {
            case DB_QUERY -> {
                this.attribute = new ProcessDbQueryAttribute(baseDir, (Map<String, Object>) spec.get("attribute"));
                this.handler = new ProcessDbQueryHandler(loadJdbcPort, type);
            }
            case OPERATOR -> {
                this.attribute = new ProcessOperatorAttribute((Map<String, Object>) spec.get("attribute"));
                this.handler = new ProcessOperatorHandler(type);
            }
        }
    }
}
