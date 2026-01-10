package com.kbank.ams.featurestreamengine.adapter.in.kafka;

import com.kbank.ams.featurestreamengine.application.port.in.DetectionUseCase;
import com.kbank.ams.featurestreamengine.application.service.DetectionService;
import com.kbank.ams.featurestreamengine.common.annotations.KafkaSubscribeAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
@KafkaSubscribeAdapter
@RequiredArgsConstructor
public class RawTxLogConsumer {
    private final DetectionUseCase detectionUseCase;

    @KafkaListener(topics = "raw-tx-log", containerFactory = "batchKafkaListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, Map<String,Object>>> records) {
        log.info("batch received size={}", records.size());

        int nullCount = 0;
        List<Map<String,Object>> items = new ArrayList<>(records.size());

        for (ConsumerRecord<String, Map<String,Object>> r : records) {
            Map<String,Object> v = r.value();
            if (v == null) {
                nullCount++;
                log.warn("discard null value (tombstone/deser). topic={}, partition={}, offset={}, key={}",
                        r.topic(), r.partition(), r.offset(), r.key());
                continue;
            }
            // 필요하면 uuid 로그
            items.add(v);
        }

        if (nullCount > 0) {
            log.warn("batch contained null values count={}", nullCount);
        }

        try {
            detectionUseCase.detect(items);
            log.info("batch processed. size={}", items.size());
        } catch (Exception e) {
            // 배치 전체 실패 (재처리/에러핸들러로 넘김)
            log.error("batch detect failed. firstOffset={} lastOffset={}",
                    records.get(0).offset(), records.get(records.size()-1).offset(), e);
            throw e;
        }
    }

}
