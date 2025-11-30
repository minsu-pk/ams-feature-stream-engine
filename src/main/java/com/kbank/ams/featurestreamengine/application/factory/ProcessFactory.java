package com.kbank.ams.featurestreamengine.application.factory;

import com.kbank.ams.featurestreamengine.application.port.out.LoadJdbcPort;
import com.kbank.ams.featurestreamengine.common.annotations.Factory;
import com.kbank.ams.featurestreamengine.domain.flow.process.Process;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Factory
@RequiredArgsConstructor
public class ProcessFactory {
    private final LoadJdbcPort loadJdbcPort;
    public Process create(
        String baseDir,
        Map<String,Object> spec
    ){
        return new Process(baseDir, spec, loadJdbcPort);
    }

}
