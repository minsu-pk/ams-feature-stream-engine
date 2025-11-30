package com.kbank.ams.featurestreamengine.application.port.in;

import java.util.List;
import java.util.Map;

public interface DetectionUseCase {
    void detect(List<Map<String,Object>> items);
}
