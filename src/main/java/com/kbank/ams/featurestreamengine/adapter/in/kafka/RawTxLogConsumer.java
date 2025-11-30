package com.kbank.ams.featurestreamengine.adapter.in.kafka;

import com.kbank.ams.featurestreamengine.application.port.in.DetectionUseCase;
import com.kbank.ams.featurestreamengine.application.service.DetectionService;
import com.kbank.ams.featurestreamengine.common.annotations.KafkaSubscribeAdapter;
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

    @KafkaListener(
        topics = "raw-tx-log",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void listen(
        List<ConsumerRecord<String, Map<String,Object>>> records
    ) {
        log.info("record.size: {}", records.size());
        detectionUseCase.detect(records.stream().map(ConsumerRecord::value).toList());
        //ack.acknowledge();
    }
}
