package no.digdir.logging.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
public class DefaultEventLogger implements EventLogger {
    private final EventLoggingConfig config;
    private final Producer<String, SpecificRecordBase> kafkaProducer;
    @Getter
    private final ExecutorService executorService;

    public DefaultEventLogger(EventLoggingConfig config) {
        this(config, new EventLoggerKafkaProducer(config), new EventLoggerThreadPoolExecutor(config));
    }

    @Override
    public void log(EventRecordBase eventRecord) {
        executorService.submit(new KafkaTask(eventRecord.toProducerRecord(config), kafkaProducer));
    }

    @Override
    public Map<MetricName, ? extends Metric> getMetrics() {
        return kafkaProducer.metrics();
    }

    @PreDestroy
    void preDestroy() {
        if (kafkaProducer != null) {
            try {
                kafkaProducer.close();
            } catch (Exception e) {
                log.warn("Failed to close Kafka producer", e);
            }
        }
    }
}
