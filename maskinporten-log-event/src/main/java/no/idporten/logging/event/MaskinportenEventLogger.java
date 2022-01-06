package no.idporten.logging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.idporten.logging.event.EventLogger.buildThreadFactory;
import static no.idporten.logging.event.EventLogger.createSendTask;
import static no.idporten.logging.event.EventLogger.resolveProducer;

@Slf4j
public class MaskinportenEventLogger {
    final ExecutorService pool;

    private final EventLoggingConfig config;
    Producer<String, MaskinportenEventRecord> producer;

    public MaskinportenEventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;
        this.producer = resolveProducer(config);
        this.pool = Executors.newFixedThreadPool(config.getThreadPoolSize(), buildThreadFactory());
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

        Runnable task = createSendTask(producerRecord, producer);
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
