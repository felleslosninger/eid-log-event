package no.digdir.logging.event;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.Map;

@Getter
public abstract class EventRecordBase {
    private final String eventName;
    private final Instant eventCreated;
    private final String eventDescription;
    private final String correlationId;
    private final Map<String, String> extraData;

    private String kafkaTopicDestination;
    private String applicationName;
    private String applicationEnvironment;

    protected EventRecordBase(
            String eventName,
            String eventDescription,
            String correlationId,
            Map<String, String> extraData,
            Instant eventCreated) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(eventName), "eventName is a required field");
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.correlationId = correlationId;
        this.extraData = extraData;
        if (eventCreated != null) {
            this.eventCreated = eventCreated;
        } else {
            this.eventCreated = Instant.now();
        }
    }

    ProducerRecord<String, SpecificRecordBase> toProducerRecord(EventLoggingConfig eventLoggingConfig) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(eventLoggingConfig.getEnvironmentName()), "No application environment set");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(eventLoggingConfig.getApplicationName()), "No application name set");

        this.applicationName = eventLoggingConfig.getApplicationName();
        this.applicationEnvironment = eventLoggingConfig.getEnvironmentName();

        if (this instanceof ActivityRecord) {
            this.kafkaTopicDestination = eventLoggingConfig.getActivityRecordTopic();
        } else if (this instanceof MPAuthenticationRecord) {
            this.kafkaTopicDestination = eventLoggingConfig.getMaskinportenAuthenticationRecordTopic();
        } else if (this instanceof MPTokenIssuedRecord) {
            this.kafkaTopicDestination = eventLoggingConfig.getMaskinportenTokenRecordTopic();
        } else {
            throw new IllegalStateException(String.format("Event type not supported: %s", this.getClass()));
        }

        return new ProducerRecord<>(kafkaTopicDestination, toAvroObject());
    }

    protected abstract SpecificRecordBase toAvroObject();
}
