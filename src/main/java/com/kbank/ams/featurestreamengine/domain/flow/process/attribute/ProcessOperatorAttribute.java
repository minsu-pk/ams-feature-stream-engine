package com.kbank.ams.featurestreamengine.domain.flow.process.attribute;


import com.kbank.ams.featurestreamengine.domain.flow.FlowModel;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class ProcessOperatorAttribute implements ProcessAttribute {
    private final List<FlowModel.Expr> exprs;
    public ProcessOperatorAttribute(Map<String,Object> spec) {
        List<Map<String,Object>> exprSpecs = (List<Map<String,Object>>) spec.get("exprs");
        this.exprs = exprSpecs.stream().map(FlowModel.Expr::new).toList();
    }
}
