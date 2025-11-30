package com.kbank.ams.featurestreamengine.domain.flow.process.handler;

import com.kbank.ams.featurestreamengine.common.util.ExpressionEvaluator;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum;
import com.kbank.ams.featurestreamengine.domain.flow.FlowEnum.ProcessType;
import com.kbank.ams.featurestreamengine.domain.flow.FlowModel.Expr;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessAttribute;
import com.kbank.ams.featurestreamengine.domain.flow.process.attribute.ProcessOperatorAttribute;
import java.util.Map;

public final class ProcessOperatorHandler implements ProcessHandler {
    private final FlowEnum.ProcessType type;

    public ProcessOperatorHandler(ProcessType type) {
        this.type = type;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> item, ProcessAttribute attribute) {
        ProcessOperatorAttribute operatorAttribute = (ProcessOperatorAttribute) attribute;
        switch (type) {
            case DERIVED -> {
                for (Expr expr : operatorAttribute.getExprs()) {
                    item.put(expr.getAs(), ExpressionEvaluator.evaluate(expr.getExpr(), item));
                }
                return item;
            }
            case FILTER -> {
                return operatorAttribute.getExprs().stream().allMatch(expr -> ExpressionEvaluator.matches(expr.getExpr(), item)) ? item : null;
            }
            default -> {
                return item;
            }
        }
    }
}
