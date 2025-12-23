package com.kbank.ams.featurestreamengine.domain.fraudscoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class FraudScoringOutput {
    private String uuid;
    private String modelName;
    private String modelVersion;
    private String prediction;
    private Double probability;
    private Double threshold;
    private Map<String, Object> features;

    @JsonCreator
    public FraudScoringOutput(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("probability") double probability,
            @JsonProperty("prediction") int prediction,
            @JsonProperty("threshold") double threshold,
            @JsonProperty("model_name") String modelName,
            @JsonProperty("model_version") String modelVersion
    ) {
        this.uuid = uuid;
        this.probability = probability;
        this.prediction = String.valueOf(prediction);
        this.threshold = threshold;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }

    public void setFeatures(Map<String, Object> features) {
        this.features = features;
    }
}
