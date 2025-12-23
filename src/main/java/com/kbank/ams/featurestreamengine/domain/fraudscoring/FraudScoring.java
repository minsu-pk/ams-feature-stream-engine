package com.kbank.ams.featurestreamengine.domain.fraudscoring;

import com.kbank.ams.featurestreamengine.common.util.JsonUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class FraudScoring {
    private String dt;
    private String eventName;
    private String uuid;
    private String custId;
    private String acctNbr;
    private String modelName;
    private String modelVersion;
    private String prediction;
    private Double probability;
    private Double threshold;
    private Map<String,Object> features;
    private String featuresJsonStr;

    @SneakyThrows
    public FraudScoring(FraudScoringIdentifier identifier, FraudScoringOutput output){
        this.uuid = output.getUuid();
        this.dt = identifier.getDt();
        this.eventName = identifier.getEventName();
        this.custId = identifier.getCustId();
        this.acctNbr = identifier.getAcctNbr();
        this.modelName = output.getModelName();
        this.modelVersion = output.getModelVersion();
        this.prediction = output.getPrediction();
        this.probability = output.getProbability();
        this.threshold = output.getThreshold();
        this.features = output.getFeatures();
        this.featuresJsonStr = JsonUtil.om.writeValueAsString(this.features);
    }
}
