package com.kbank.ams.featurestreamengine.domain.fraudscoring;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class FraudScoringIdentifier {
    private String dt;
    private String eventName;
    private String uuid;
    private String custId;
    private String acctNbr;

    @Builder
    public FraudScoringIdentifier(String dt, String eventName, String uuid, String custId, String acctNbr) {
        this.dt = dt;
        this.eventName = eventName;
        this.uuid = uuid;
        this.custId = custId;
        this.acctNbr = acctNbr;
    }
}
