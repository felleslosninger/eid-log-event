package no.idporten.logging.event;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static no.idporten.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
public class EventLogger {
    private static final String SEPARATOR = ";";
    final ExecutorService pool;

    private final EventLoggingConfig config;
    Producer<String, EventRecord> producer;

    public EventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;
        this.producer = resolveProducer(config);
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

    private static EventRecord enrichRecord(EventRecord eventRecord, EventLoggingConfig config) {
        return EventRecord.newBuilder(eventRecord)
                .setApplication(eventRecord.getApplication() == null ?
                        config.getApplicationName() : eventRecord.getApplication())
                .setEnvironment(eventRecord.getEnvironment() == null ?
                        config.getEnvironmentName() : eventRecord.getEnvironment())
                .setCreated(eventRecord.getCreated() == null ? Instant.now() : eventRecord.getCreated())
                .build();
    }

    static <T extends SpecificRecordBase> Runnable createSendTask(
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

    public void log(EventRecord eventRecord) {
        EventRecord enrichedRecord = enrichRecord(eventRecord, config);
        ProducerRecord<String, EventRecord> producerRecord =
                new ProducerRecord<>(
                        config.getEventTopic(),
                        createKafkaKey(enrichedRecord),
                        enrichedRecord);

        Runnable task = createSendTask(producerRecord, producer);
        pool.submit(task);
    }

    /**
     * The kafka key is generated based upon all "dimensions" of the eventRecord.
     * This ensures that all records that should be counted together (statistics-wise), will
     * end up in the same partition and can be aggregated using kafka-streams.
     *
     * @param eventRecord the eventRecord that the key should be based upon.
     * @return a composite kafkaKey based on all dimensions of the eventRecord.
     */
    static String createKafkaKey(EventRecord eventRecord) {
        Objects.requireNonNull(eventRecord, "EventRecord was null, cannot create a KafkaKey from a null-object.");
        return eventRecord.getName() + SEPARATOR +
                eventRecord.getApplication() + SEPARATOR +
                eventRecord.getClient() + SEPARATOR +
                eventRecord.getRepresenting() + SEPARATOR +
                eventRecord.getEid() + SEPARATOR +
                eventRecord.getAuthmethod() + SEPARATOR +
                eventRecord.getEnvironment() + SEPARATOR;
    }

    @Override
    protected void finalize() {
        if (producer != null) {
            try {
                producer.close();
            } catch (Exception e) {
                log.warn("Failed to close Kafka producer", e);
            }
        }
    }

    public Map<MetricName, ? extends Metric> getMetrics() {
        return producer.metrics();
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
