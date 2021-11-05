package no.idporten.logging.event;

import lombok.extern.slf4j.Slf4j;
import no.idporten.logging.event.config.EventLoggingConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.concurrent.ExecutorService;

@Slf4j
public class EventLogger {
    private final ExecutorService executorService;
    private final EventLoggingConfig config;
    Producer<String, EventRecord> producer;

    public EventLogger(EventLoggingConfig eventLoggingConfig, ExecutorService executorService) {
        this.config = eventLoggingConfig;
        this.producer = new KafkaProducer<>(config.toMap());
        this.executorService = executorService;
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

        executorService.submit(task);
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
