package no.idporten.logging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.idporten.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
public class EventLogger {
    final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final EventLoggingConfig config;
    Producer<String, EventRecord> producer;

    public EventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;

        if (config.isFeatureEnabled()) {
            this.producer = new KafkaProducer<>(config.toMap());
        } else {
            this.producer = new NoLoggingProducer();
            log.info("Event logging disabled through property {}={}", FEATURE_ENABLED_KEY, config.isFeatureEnabled());
        }
    }

    public void log(EventRecord eventRecord) {
        ProducerRecord<String, EventRecord> producerRecord =
                new ProducerRecord<>(config.getEventTopic(), eventRecord.getPid().toString(), eventRecord);

        Runnable task = () -> {
            try {
                producer.send(producerRecord);
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
