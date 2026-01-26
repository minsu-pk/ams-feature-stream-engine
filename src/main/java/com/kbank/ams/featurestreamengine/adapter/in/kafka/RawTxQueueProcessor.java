package com.kbank.ams.featurestreamengine.adapter.in.kafka;

import com.kbank.ams.featurestreamengine.application.port.in.DetectionUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class RawTxQueueProcessor {
    private static final int QUEUE_CAPACITY = 500_000;
    private static final int MICRO_BATCH_SIZE = 100;
    private static final long MICRO_BATCH_MAX_WAIT_MS = 1000;
    private final BlockingQueue<WorkerItem> queue = new ArrayBlockingQueue(QUEUE_CAPACITY);
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final DetectionUseCase detectionUseCase;

    public RawTxQueueProcessor(DetectionUseCase detectionUseCase) {
        this.detectionUseCase = detectionUseCase;
    }

    public boolean enqueue(String topic, int partition, long offset, String key, Map<String,Object> value) {
        WorkerItem item = new WorkerItem(topic, partition, offset, key, value);
        try {
            return queue.offer(item, MICRO_BATCH_MAX_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void startWorker() {
        worker.scheduleAtFixedRate(this::drainAndProcess, 0, 10, TimeUnit.MILLISECONDS);
    }

    private void drainAndProcess() {
        try {
            List<Map<String,Object>> items = new ArrayList<>(MICRO_BATCH_SIZE);
            List<WorkerItem> metas = new ArrayList<>();

            WorkerItem first = queue.poll(MICRO_BATCH_MAX_WAIT_MS, TimeUnit.MILLISECONDS);
            if (first==null) return;

            metas.add(first);
            items.add(first.value);

            queue.drainTo(metas, MICRO_BATCH_SIZE -1);
            for (int i =1; i < metas.size(); i++) {
                items.add(metas.get(i).value);
            }

            long firstOffset = metas.get(0).offset;
            long lastOffset = metas.get(metas.size() - 1).offset;

        } catch (Exception e) {

        }
    }


    private static final class WorkerItem {
        final String topic;
        final int partition;
        final long offset;
        final String key;
        final Map<String, Object> value;

        public WorkerItem(String topic, int partition, long offset, String key, Map<String, Object> value) {
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.key = key;
            this.value = value;
        }
    }
}

