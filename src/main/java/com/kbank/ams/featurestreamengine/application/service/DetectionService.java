package com.kbank.ams.featurestreamengine.application.service;

import com.kbank.ams.featurestreamengine.application.factory.FlowFactory;
import com.kbank.ams.featurestreamengine.application.port.in.DetectionUseCase;
import com.kbank.ams.featurestreamengine.application.port.out.FraudScoringPort;
import com.kbank.ams.featurestreamengine.application.port.out.StoreJdbcPort;
import com.kbank.ams.featurestreamengine.common.annotations.UseCase;
import com.kbank.ams.featurestreamengine.domain.command.FraudScoringCommand;
import com.kbank.ams.featurestreamengine.domain.command.StoreJdbcCommand;
import com.kbank.ams.featurestreamengine.domain.flow.Flow;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoring;
import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringIdentifier;
import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringInput;
import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class DetectionService<T> implements DetectionUseCase {
    @Qualifier("flowNames")
    private final List<String> flowNames;
    private final FlowFactory flowFactory;
    private final FraudScoringPort fraudScoringPort;
    private final StoreJdbcPort storeJdbcPort;

    private final Map<String, Flow> flowRepository = new HashMap<>();
    @Value("${ams.feature-stream-engine.exclude-from-features}")
    private String excludeFromFeaturesStr;
    private List<String> excludeFromFeatures;

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

        /*
        *  EXCLUDE_FROM_FEATURES AND FRAUD_SCORING INPUT SETTING
        */
        log.info("extractedFeatures : {}", extractedFeatures);
        List<FraudScoringInput> inputs = extractedFeatures.stream()
                .map(item -> {
                    String uuid = item.get("uuid").toString();
                    Map<String,Object> features = item.entrySet().stream()
                            .filter(e -> !excludeFromFeatures.contains(e.getKey()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ));
                    return FraudScoringInput.builder().uuid(uuid).features(features).build();
                }).toList();

        /*
         *  IDENTIFIER MAP
         */
        Map<String, FraudScoringIdentifier> identifierMap = extractedFeatures.stream()
                .map(item -> {
                    String uuid = item.get("uuid").toString();
                    String dt = item.get("dt").toString();
                    String custId = item.get("cust_id").toString();
                    String acctNbr = item.get("acct_nbr").toString();
                    String eventName = "raw-tx-log";
                    return FraudScoringIdentifier.builder()
                            .uuid(uuid)
                            .eventName(eventName)
                            .custId(custId)
                            .acctNbr(acctNbr)
                            .dt(dt)
                            .build();
                }).collect(Collectors.toMap(FraudScoringIdentifier::getUuid, Function.identity()));

        /*
         *  FRAUD SCORING OUTPUTS
         */
        List<FraudScoringOutput> outputs = fraudScoringPort.score(inputs);

        List<FraudScoring> fraudScorings = outputs.stream().map(output -> new FraudScoring(identifierMap.get(output.getUuid()), output)).toList();

        log.info("fraudScorings : {}", fraudScorings);
        storeJdbcPort.store(StoreJdbcCommand.<FraudScoring>builder().updateSql(INSERT_FRAUD_SCORING_SQL).items(fraudScorings).build());
    }

    @PostConstruct
    public void init(){
        pool = new ForkJoinPool(4);

        for (String flowName : flowNames) {
            flowRepository.put(flowName, flowFactory.create(flowName));
        }

        this.excludeFromFeatures = Arrays.stream(excludeFromFeaturesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String INSERT_FRAUD_SCORING_SQL = """
        INSERT INTO fraud_scoring_result (
            created_at, uuid, event_name, dt, cust_id, acct_nbr, model_name, model_version, prediction, probability, threshold, features
        ) VALUES (
            now(), :uuid, :eventName, :dt, :custId, :acctNbr, :modelName, :modelVersion, :prediction, :probability, :threshold, :featuresJsonStr
        )
            """;
}
