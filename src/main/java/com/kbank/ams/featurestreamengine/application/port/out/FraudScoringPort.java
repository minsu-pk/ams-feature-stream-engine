package com.kbank.ams.featurestreamengine.application.port.out;

import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringInput;
import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringOutput;

import java.util.List;

public interface FraudScoringPort {
    List<FraudScoringOutput> score(List<FraudScoringInput> inputs);
}
