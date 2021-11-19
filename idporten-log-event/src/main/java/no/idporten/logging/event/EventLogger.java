package no.idporten.logging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static no.idporten.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
public class EventLogger {
    final ThreadPoolExecutor pool;

    private final EventLoggingConfig config;
    Producer<String, EventRecord> producer;

    public EventLogger(EventLoggingConfig eventLoggingConfig) {
        this.config = eventLoggingConfig;

        if (config.isFeatureEnabled()) {
            this.producer = new KafkaProducer<>(config.getProducerConfig());
        } else {
            this.producer = new NoLoggingProducer();
            log.info("Event logging disabled through property {}={}", FEATURE_ENABLED_KEY, config.isFeatureEnabled());
        }

        pool = new ThreadPoolExecutor(
                config.getThreadPoolSize(),
                config.getThreadPoolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );
    }

    public void log(EventRecord eventRecord) {
        ProducerRecord<String, EventRecord> producerRecord =
                new ProducerRecord<>(config.getEventTopic(), eventRecord.getPid().toString(), eventRecord);

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

    public Map<MetricName, ? extends Metric> getMetrics() {
        return producer.metrics();
    }

    public String getPoolQueueStats() {
        return String.format(
                "ThreadPoolSize: %d, activeCount: %d, queueSize: %d",
                pool.getPoolSize(),
                pool.getActiveCount(),
                pool.getQueue().size());
    }
}
