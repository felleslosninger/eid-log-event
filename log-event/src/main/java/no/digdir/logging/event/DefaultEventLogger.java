package no.digdir.logging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
class DefaultEventLogger implements EventLogger {
    private final EventLoggingConfig config;
    private final Producer<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;
    @Getter
    private final ExecutorService executorService;

    DefaultEventLogger(EventLoggingConfig config) {
        this(config, new EventLoggerKafkaProducer(config), new EventLoggerThreadPoolExecutor(config));
    }

    public DefaultEventLogger(
            EventLoggingConfig config,
            Producer<String, String> kafkaProducer,
            ExecutorService executorService) {
        this.config = config;
        this.kafkaProducer = kafkaProducer;
        this.executorService = executorService;
        this.objectMapper = configureObjectMapper();
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS); // <-- Millis since epoch
        return objectMapper;
    }

    @Override
    public void log(EventRecordBase eventRecord) {
        try {
            executorService.submit(new KafkaTask(toProducerRecord(eventRecord), kafkaProducer))  ;
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event record", e);
        }
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

    ProducerRecord<String, String> toProducerRecord(EventRecordBase eventRecord) throws JsonProcessingException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getEnvironmentName()), "No application environment set");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getApplicationName()), "No application name set");
        String kafkaTopicDestination;

        eventRecord.setApplicationName(config.getApplicationName());
        eventRecord.setApplicationEnvironment(config.getEnvironmentName());

        if (eventRecord instanceof ActivityRecord) {
            kafkaTopicDestination = config.getActivityRecordTopic();
        } else if (eventRecord instanceof MPAuthenticationRecord) {
            kafkaTopicDestination = config.getMaskinportenAuthenticationRecordTopic();
        } else if (eventRecord instanceof MPTokenIssuedRecord) {
            kafkaTopicDestination = config.getMaskinportenTokenRecordTopic();
        } else {
            throw new IllegalStateException(String.format("Event type not supported: %s", this.getClass()));
        }

        return new ProducerRecord<>(kafkaTopicDestination, objectMapper.writeValueAsString(eventRecord));
    }
}
