package no.digdir.logging.event;

import no.digdir.logging.event.generated.MaskinPortenTokenIssuedAvro;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MPTokenIssuedRecordTest {
    @Test
    void topicFromCorrectConfig() {
        String topicName = "correctTopic";

        MPTokenIssuedRecord record = MPTokenIssuedRecord.builder()
                .eventName("Token Issued")
                .eventDescription("Description")
                .correlationId("correlationId")
                .eventCreated(Instant.now().minus(Duration.ofSeconds(60)))
                .extraData(Collections.singletonMap("key", "value"))
                .clientId("clientId")
                .clientOrgno("clientOrgno")
                .clientOnBehalfOfId("clientOnBehalfOfId")
                .tokenIss("tokenIss")
                .tokenLifetimeSeconds(123)
                .tokenScopes(Collections.singletonList(
                        new MPTokenIssuedRecord.TokenScope("scope", "delegationSource")
                ))
                .supplier("supplier")
                .consumer("consumer")
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .applicationName("application")
                .environmentName("environment")
                .activityRecordTopic("wrongTopic")
                .maskinportenAuthenticationRecordTopic("wrongTopic")
                .maskinportenTokenRecordTopic(topicName)
                .build();

        ProducerRecord<String, SpecificRecordBase> result = record.toProducerRecord(config);
        assertEquals(topicName, result.topic());
    }

    @Test
    void toAvroObject() {
        MPTokenIssuedRecord record = MPTokenIssuedRecord.builder()
                .eventName("Token Issued")
                .eventDescription("Description")
                .correlationId("correlationId")
                .extraData(Collections.singletonMap("key", "value"))
                .clientId("clientId")
                .clientOrgno("clientOrgno")
                .clientOnBehalfOfId("clientOnBehalfOfId")
                .tokenIss("tokenIss")
                .tokenLifetimeSeconds(123)
                .tokenScopes(Collections.singletonList(
                        new MPTokenIssuedRecord.TokenScope("scope", "delegationSource")
                ))
                .supplier("supplier")
                .consumer("consumer")
                .eventCreated(Instant.now().minus(Duration.ofSeconds(60)))
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .activityRecordTopic("activityTopic")
                .applicationName("applicationName")
                .environmentName("applicationEnvironment").build();

        MaskinPortenTokenIssuedAvro avroRecord = (MaskinPortenTokenIssuedAvro) record.toProducerRecord(config).value();

        assertEquals(record.getEventName(), avroRecord.getEventName());
        assertEquals(record.getEventDescription(), avroRecord.getEventDescription());
        assertEquals(record.getClientId(), avroRecord.getClientId());
        assertEquals(record.getClientOrgno(), avroRecord.getClientOrgno());
        assertEquals(record.getClientOnBehalfOfId(), avroRecord.getClientOnBehalfOfId());
        assertEquals(record.getTokenIss(), avroRecord.getTokenIss());
        assertEquals(record.getTokenLifetimeSeconds(), avroRecord.getTokenLifetimeSeconds());
        assertEquals(record.getTokenScopes().size(), avroRecord.getTokenScopes().size());
        assertEquals(record.getTokenScopes().get(0).getScope(), avroRecord.getTokenScopes().get(0).getScope());
        assertEquals(record.getTokenScopes().get(0).getDelegationSource(), avroRecord.getTokenScopes()
                .get(0)
                .getDelegationSource());
        assertEquals(record.getSupplier(), avroRecord.getSupplier());
        assertEquals(record.getConsumer(), avroRecord.getConsumer());
        assertEquals(record.getCorrelationId(), avroRecord.getCorrelationId());
        assertEquals(record.getExtraData(), avroRecord.getExtraData());

        assertEquals(record.getApplicationEnvironment(), avroRecord.getApplicationEnvironment());
        assertEquals(record.getApplicationName(), avroRecord.getApplicationName());
        assertEquals(record.getEventCreated().toEpochMilli(), avroRecord.getEventCreated().toEpochMilli());
    }
}