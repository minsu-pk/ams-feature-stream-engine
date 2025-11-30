package com.kbank.ams.featurestreamengine.domain.flow.process.handler;

import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessAttribute;
import java.util.Map;

public sealed interface ProcessHandler permits ProcessDbQueryHandler, ProcessOperatorHandler {
    Map<String, Object> handle(Map<String, Object> item, ProcessAttribute attribute);
}
