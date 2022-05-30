package no.digdir.logging.event;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static no.digdir.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
public class EventLogger {
    final ExecutorService pool;

    private final EventLoggingConfig config;
    Producer<String, ActivityRecord> activityProducer;
    Producer<String, MaskinportenAuthentication> mpAuthenticationProducer;
    Producer<String, MaskinPortenTokenIssued> mpTokenIssuedProducer;

    public EventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;
        this.activityProducer = resolveProducer(config);
        this.mpAuthenticationProducer = resolveProducer(config);
        this.mpTokenIssuedProducer = resolveProducer(config);
        this.pool = Executors.newFixedThreadPool(config.getThreadPoolSize(), buildThreadFactory());
    }

    static <T extends SpecificRecordBase> Producer<String, T> resolveProducer(EventLoggingConfig config) {
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

    static <T extends EventRecordBase> Runnable createSendTask(
            ProducerRecord<String, T> producerRecord,
            Producer<String, T> producer) {

        return () -> {
            try {
                producer.send(producerRecord, (recordMetadata, e) -> {
                    if (e != null) {
                        log.warn("Failed to publish event {}", producerRecord.value(), e);
                    } else if (log.isTraceEnabled() && recordMetadata != null) {
                        log.trace("Sent record {} with offset {}", producerRecord, recordMetadata.offset());
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to publish event {}", producerRecord.value(), e);
            }
        };
    }

    private static <T extends EventRecordBase> T enrichRecord(T record, EventLoggingConfig config) {
        record.setApplicationName(record.getApplicationName() == null ?
                config.getApplicationName() : record.getApplicationName());
        record.setApplicationEnvironment(record.getApplicationEnvironment() == null ?
                config.getEnvironmentName() : record.getApplicationEnvironment());
        record.setEventCreated(record.getEventCreated() == null ? Instant.now() : record.getEventCreated());
        return record;
    }

    public void log(EventRecordBase eventRecord) {
        EventRecordBase enrichedRecord = enrichRecord(eventRecord, config);
        Runnable task;

        if (eventRecord instanceof ActivityRecord) {
            ProducerRecord<String, ActivityRecord> producerRecord =
                    new ProducerRecord<>(config.getEventTopic(), (ActivityRecord) enrichedRecord);
            task = createSendTask(producerRecord, activityProducer);

        } else if (eventRecord instanceof MaskinportenAuthentication) {
            ProducerRecord<String, MaskinportenAuthentication> producerRecord =
                    new ProducerRecord<>(config.getMpAuthenticationTopic(), (MaskinportenAuthentication) enrichedRecord);
            task = createSendTask(producerRecord, mpAuthenticationProducer);

        } else if (eventRecord instanceof MaskinPortenTokenIssued) {
            ProducerRecord<String, MaskinPortenTokenIssued> producerRecord =
                    new ProducerRecord<>(config.getEventTopic(), (MaskinPortenTokenIssued) enrichedRecord);
            task = createSendTask(producerRecord, mpTokenIssuedProducer);

        } else {
            throw new IllegalStateException(String.format("Event type not supported: %s", eventRecord.getClass()));
        }

        pool.submit(task);
    }

    @Override
    protected void finalize() {
        if (activityProducer != null) {
            try {
                activityProducer.close();
            } catch (Exception e) {
                log.warn("Failed to close Kafka producer", e);
            }
        }
    }

    public Map<MetricName, ? extends Metric> getMetrics() {
        return activityProducer.metrics();
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
