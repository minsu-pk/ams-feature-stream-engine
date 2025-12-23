package com.kbank.ams.featurestreamengine.domain.fraudscoring;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class FraudScoringInput {
    private String uuid;
    private Map<String,Object> features;

    @Builder
    public FraudScoringInput(String uuid, Map<String, Object> features) {
        this.uuid = uuid;
        this.features = features;
    }
}
