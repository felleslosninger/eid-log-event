package no.idporten.logging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.idporten.logging.event.EventLogger.buildThreadFactory;
import static no.idporten.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
public class MaskinportenEventLogger {
    final ExecutorService pool;

    private final EventLoggingConfig config;
    Producer<String, MaskinportenEventRecord> producer;

    public MaskinportenEventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;

        if (config.isFeatureEnabled()) {
            this.producer = new KafkaProducer<>(config.getProducerConfig());
        } else {
            this.producer = new NoLoggingProducer();
            log.info("Event logging disabled through property {}={}", FEATURE_ENABLED_KEY, config.isFeatureEnabled());
        }

        pool = Executors.newFixedThreadPool(config.getThreadPoolSize(), buildThreadFactory());
    }

    private static MaskinportenEventRecord enrichRecord(
            MaskinportenEventRecord eventRecord,
            EventLoggingConfig config) {
        return MaskinportenEventRecord.newBuilder(eventRecord)
                .setApplication(eventRecord.getApplication() == null ?
                        config.getApplicationName() : eventRecord.getApplication())
                .setEnvironment(eventRecord.getEnvironment() == null ?
                        config.getEnvironmentName() : eventRecord.getEnvironment())
                .setCreated(eventRecord.getCreated() == null ? Instant.now() : eventRecord.getCreated())
                .build();
    }

    public void log(MaskinportenEventRecord eventRecord) {
        MaskinportenEventRecord enrichedRecord = enrichRecord(eventRecord, config);
        ProducerRecord<String, MaskinportenEventRecord> producerRecord =
                new ProducerRecord<>(
                        config.getEventTopic(),
                        eventRecord.getConsumer().toString(),
                        enrichedRecord);

        Runnable task = () -> {
            try {
                producer.send(producerRecord, (recordMetadata, e) -> {
                    if (e != null) {
                        log.warn("Failed to publish event {}", eventRecord, e);
                    } else if (log.isTraceEnabled() && recordMetadata != null) {
                        log.trace("Sent record {} with offset {}", producerRecord, recordMetadata.offset());
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to publish event {}", eventRecord, e);
            }
        };

        pool.submit(task);
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

}
