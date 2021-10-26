package no.idporten.logging.event;

import lombok.extern.slf4j.Slf4j;
import no.idporten.logging.event.config.EventLoggingConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class EventLogger {
    EventLoggingConfig config;
    Producer<String, EventRecord> producer;

    public EventLogger(EventLoggingConfig eventLoggingConfig) {
        config = eventLoggingConfig;
        producer = new KafkaProducer<>(config.toMap());
    }

    public void log(EventRecord eventRecord) {
        ProducerRecord<String, EventRecord> producerRecord =
                new ProducerRecord<>(config.getEventTopic(), eventRecord.getPid().toString(), eventRecord);

        CompletableFuture
                .runAsync(() -> producer.send(producerRecord))
                .exceptionally(e -> {
                    log.warn("Failed to publish event {}", eventRecord, e);
                    return null;
                });
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
