package no.digdir.logging.event;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static no.digdir.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
public class EventLogger {
    final ExecutorService pool;

    private final EventLoggingConfig config;
    Producer<String, SpecificRecordBase> kafkaProducer;

    public EventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;
        this.kafkaProducer = resolveProducer(config);
        this.pool = generateFixedThreadPool(config);
    }

    private ExecutorService generateFixedThreadPool(EventLoggingConfig config) {
        return new ThreadPoolExecutor(config.getThreadPoolSize(), config.getThreadPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(config.getThreadPoolQueueSize()),
                buildThreadFactory(),
                new DiscardAndLogOldestPolicy());
    }

    static Producer<String, SpecificRecordBase> resolveProducer(EventLoggingConfig config) {
        if (config.isFeatureEnabled()) {
            return new KafkaProducer<>(config.getProducerConfig());
        } else {
            log.info("Event logging disabled through property {}={}", FEATURE_ENABLED_KEY, config.isFeatureEnabled());
            return new NoLoggingProducer<>();
        }
    }

    static ThreadFactory buildThreadFactory() {
        return new ThreadFactory() {
            private int threadNumber = 0;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "eventLogPool-" + threadNumber++);
            }
        };
    }

    public void log(EventRecordBase eventRecord) {
        pool.submit(new KafkaTask(eventRecord.toProducerRecord(config), kafkaProducer));
    }

    @Override
    protected void finalize() {
        if (kafkaProducer != null) {
            try {
                kafkaProducer.close();
            } catch (Exception e) {
                log.warn("Failed to close Kafka producer", e);
            }
        }
    }

    public Map<MetricName, ? extends Metric> getMetrics() {
        return kafkaProducer.metrics();
    }

    public String getPoolQueueStats() {
        if (pool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) pool;
            return String.format(
                    "ThreadPoolSize: %d, activeCount: %d, queueSize: %d",
                    threadPoolExecutor.getPoolSize(),
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getQueue().size());
        } else {
            return "Cannot get ThreadPool queueStats as ExecutorService is not of type ThreadPoolExecutor. It was type: "
                    + pool.getClass();
        }
    }
}
