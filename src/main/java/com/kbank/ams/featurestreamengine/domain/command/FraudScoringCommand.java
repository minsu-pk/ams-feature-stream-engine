package com.kbank.ams.featurestreamengine.domain.command;

import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringInput;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class FraudScoringCommand {
    private List<FraudScoringInput> inputs;

    @Builder
    public FraudScoringCommand(List<FraudScoringInput> inputs) {
        this.inputs = inputs;
    }
}
