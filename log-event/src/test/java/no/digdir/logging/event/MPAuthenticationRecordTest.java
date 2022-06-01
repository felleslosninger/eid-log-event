package no.digdir.logging.event;

import no.digdir.logging.event.generated.MaskinportenAuthenticationAvro;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MPAuthenticationRecordTest {

    @Test
    void topicFromCorrectConfig() {
        String topicName = "correctTopic";

        MPAuthenticationRecord record = MPAuthenticationRecord.builder()
                .eventName("Token Issued")
                .eventDescription("Description")
                .eventCreated(Instant.now().minus(Duration.ofSeconds(60)))
                .correlationId("correlationId")
                .extraData(Collections.singletonMap("key", "value"))
                .clientId("clientId")
                .clientOrgno("clientOrgno")
                .clientOnBehalfOfId("clientOnBehalfOfId")
                .certificateIssuer("certificateIssuer")
                .certificateSerialNumber("123")
                .kid("kid")
                .aud("aud")
                .tokenEndpointAuthMethod("TokenEndpointAuthMethod")
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .applicationName("application")
                .environmentName("environment")
                .activityRecordTopic("wrongTopic")
                .maskinportenAuthenticationRecordTopic(topicName)
                .maskinportenTokenRecordTopic("wrongTopic")
                .build();

        ProducerRecord<String, SpecificRecordBase> result = record.toProducerRecord(config);
        assertEquals(topicName, result.topic());
    }

    @Test
    void toAvroObject() {
        MPAuthenticationRecord record = MPAuthenticationRecord.builder()
                .eventName("Token Issued")
                .eventDescription("Description")
                .correlationId("correlationId")
                .extraData(Collections.singletonMap("key", "value"))
                .clientId("clientId")
                .clientOrgno("clientOrgno")
                .clientOnBehalfOfId("clientOnBehalfOfId")
                .certificateIssuer("certificateIssuer")
                .certificateSerialNumber("123")
                .eventCreated(Instant.now().minus(Duration.ofSeconds(60)))
                .kid("kid")
                .aud("aud")
                .tokenEndpointAuthMethod("TokenEndpointAuthMethod")
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .activityRecordTopic("activityTopic")
                .applicationName("applicationName")
                .environmentName("applicationEnvironment").build();

        MaskinportenAuthenticationAvro avroRecord = (MaskinportenAuthenticationAvro) record.toProducerRecord(config)
                .value();

        assertEquals(record.getEventName(), avroRecord.getEventName());
        assertEquals(record.getEventDescription(), avroRecord.getEventDescription());
        assertEquals(record.getClientId(), avroRecord.getClientId());
        assertEquals(record.getClientOrgno(), avroRecord.getClientOrgno());
        assertEquals(record.getClientOnBehalfOfId(), avroRecord.getClientOnBehalfOfId());
        assertEquals(record.getCertificateIssuer(), avroRecord.getCertificateIssuer());
        assertEquals(record.getCertificateSerialNumber(), avroRecord.getCertificateSerialNumber());
        assertEquals(record.getKid(), avroRecord.getKid());
        assertEquals(record.getAud(), avroRecord.getAud());
        assertEquals(record.getTokenEndpointAuthMethod(), avroRecord.getTokenEndpointAuthMethod());
        assertEquals(record.getCorrelationId(), avroRecord.getCorrelationId());
        assertEquals(record.getExtraData(), avroRecord.getExtraData());
        assertEquals(record.getEventCreated().toEpochMilli(), avroRecord.getEventCreated().toEpochMilli());

        assertEquals(record.getApplicationEnvironment(), avroRecord.getApplicationEnvironment());
        assertEquals(record.getApplicationName(), avroRecord.getApplicationName());
    }
}