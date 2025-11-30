package com.kbank.ams.featurestreamengine.application.service;

import com.kbank.ams.featurestreamengine.application.factory.FlowFactory;
import com.kbank.ams.featurestreamengine.application.port.in.DetectionUseCase;
import com.kbank.ams.featurestreamengine.common.annotations.UseCase;
import com.kbank.ams.featurestreamengine.domain.flow.Flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class DetectionService implements DetectionUseCase {
    @Qualifier("flowNames")
    private final List<String> flowNames;
    private final FlowFactory flowFactory;

    private final Map<String, Flow> flowRepository = new HashMap<>();

    private ForkJoinPool pool;

    @Override
    public void detect(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String DEFAULT_FLOW_NAME = "feature-stream";
        Flow flow = flowRepository.get(DEFAULT_FLOW_NAME);

        List<Map<String, Object>> extractedFeatures = pool.submit(() ->
            items.parallelStream()
                .map(item -> {
                    try {
                        return flow.run(item);  // ← 여기서 에러나도
                    } catch (Exception e) {
                        // TODO: 로그 남기기 (어떤 item 에서 어떤 에러 났는지)
                        // log.warn("Flow error for item: {}", item, e);
                        return null;          // ← 해당 아이템만 버리고 계속
                    }
                })
                .filter(Objects::nonNull)
                .toList()
        ).join();

        log.info("extractedFeatures : {}", extractedFeatures);
    }

    @PostConstruct
    public void init(){
        pool = new ForkJoinPool(4);

        for (String flowName : flowNames) {
            flowRepository.put(flowName, flowFactory.create(flowName));
        }
    }
}
