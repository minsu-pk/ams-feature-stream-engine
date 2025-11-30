package com.kbank.ams.featurestreamengine.application.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kbank.ams.featurestreamengine.common.annotations.Factory;
import com.kbank.ams.featurestreamengine.common.util.JsonUtil;
import com.kbank.ams.featurestreamengine.domain.flow.Flow;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Factory
@RequiredArgsConstructor
public class FlowFactory {
    @Value("${ams.feature-stream-engine.config-dir}")
    private String configDir;

    @Qualifier("flowSpecRepo")
    private final Map<String,String> specRepository;
    private final ProcessFactory processFactory;

    @SneakyThrows
    public Flow create(String apiPath){
        Map<String,Object> spec = JsonUtil.om.readValue(specRepository.get(apiPath), new TypeReference<Map<String, Object>>() {});
        String baseDir = configDir + "/flow/" + apiPath + "/";
        Flow flow = new Flow(apiPath, spec);
        if (spec.get("processes") != null) {
            List<Map<String,Object>> processSpecs = (List<Map<String,Object>>) spec.get("processes");
            processSpecs.stream().map(processSpec -> processFactory.create(baseDir, processSpec)).forEach(flow::addProcess);
        }
        return flow;
    }
}
