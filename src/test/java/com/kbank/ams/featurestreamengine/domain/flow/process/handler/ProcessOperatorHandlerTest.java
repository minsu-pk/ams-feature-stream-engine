package com.kbank.ams.featurestreamengine.domain.flow.process.handler;

import com.kbank.ams.featurestreamengine.common.util.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
class ProcessOperatorHandlerTest {

    @Test
    public void run01(){
        List<Map<String,Object>> items = new ArrayList<>();
        Map<String,Object> item01 = new HashMap<>();
        item01.put("tx_amt", 10000);
        item01.put("cust_id", "0000012345");
        item01.put("wd_prsn_nm", "kaka(company)");
        item01.put("dp_prsn_nm", "kaka");
        item01.put("null_test", null);

        Map<String,Object> item02 = new HashMap<>();
        item02.put("tx_amt", 20000);
        item02.put("cust_id", "0000012340");
        item02.put("wd_prsn_nm", "ronaldo");
        item02.put("dp_prsn_nm", "messi");
        item02.put("null_test", null);

        items.add(item01);
        items.add(item02);

        String expr = ":tx_amt >= 10000";
        //String expr2 = ":cust_id = '0000012345'";
        String expr3 = ":dp_prsn_nm NOT LIKE CONCAT('%', :wd_prsn_nm, '%') AND :wd_prsn_nm NOT LIKE CONCAT('%', :dp_prsn_nm, '%')";

        List<Map<String,Object>> filtered = items.stream().filter(tem -> ExpressionEvaluator.matches(expr, tem)).toList();

        String exprDerived = "IF(:dp_prsn_nm NOT LIKE CONCAT('%', :wd_prsn_nm, '%') AND :wd_prsn_nm NOT LIKE CONCAT('%', :dp_prsn_nm, '%'), '1', '0')";
        String exprDerived2 = "NVL(:null_test, 0)";
        filtered.stream().map(item -> {
            item.put("is_tx_amt", ExpressionEvaluator.evaluate(exprDerived, item));
            item.put("null_test", ExpressionEvaluator.evaluate(exprDerived2, item));
            return item;
        }).toList();
        //filtered = filtered.stream().filter(tem -> ExpressionEvaluator.matches(expr2, tem)).toList();
        //filtered = filtered.stream().filter(tem -> ExpressionEvaluator.matches(expr3, tem)).toList();
        log.info("filtered: {}", filtered);
    }
}