package no.digdir.logging.event;

import no.digdir.logging.event.generated.ActivityRecordAvro;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityRecordTest {

    private static final String FNR = "25079418415";

    @Test
    void applicationNameAlwaysFromConfig() {
        String applicationName = "unitTest";
        String topicName = "kafkaTopic";
        String applicationEnvironment = "PROD";

        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid(FNR)
                .correlationId(UUID.randomUUID().toString())
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .activityRecordTopic(topicName)
                .applicationName(applicationName)
                .environmentName(applicationEnvironment).build();


        ProducerRecord<String, SpecificRecordBase> result = record.toProducerRecord(config);
        assertEquals(topicName, result.topic());
        assertTrue(result.value() instanceof ActivityRecordAvro);
        ActivityRecordAvro activityRecordAvro = (ActivityRecordAvro) result.value();
        assertEquals(applicationName, activityRecordAvro.getApplicationName());
        assertEquals(applicationEnvironment, activityRecordAvro.getApplicationEnvironment());
    }

    @Test
    void applicationEnvironmentAlwaysFromConfig() {
        String applicationEnvironment = "PROD";

        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid(FNR)
                .correlationId(UUID.randomUUID().toString())
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .applicationName("application")
                .environmentName(applicationEnvironment).build();

        ProducerRecord<String, SpecificRecordBase> result = record.toProducerRecord(config);
        assertTrue(result.value() instanceof ActivityRecordAvro);
        ActivityRecordAvro activityRecordAvro = (ActivityRecordAvro) result.value();
        assertEquals(applicationEnvironment, activityRecordAvro.getApplicationEnvironment());
    }

    @Test
    void topicFromCorrectConfig() {
        String topicName = "activityRecordTopic";

        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid(FNR)
                .correlationId(UUID.randomUUID().toString())
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .applicationName("applicationName")
                .environmentName("test")
                .activityRecordTopic(topicName)
                .maskinportenAuthenticationRecordTopic("wrongTopic")
                .maskinportenTokenRecordTopic("wrongTopic")
                .build();

        ProducerRecord<String, SpecificRecordBase> result = record.toProducerRecord(config);
        assertEquals(topicName, result.topic());
    }

    @Test
    void getDescription() {
        String description = "Brukeren har logget inn";

        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid(FNR)
                .correlationId(UUID.randomUUID().toString())
                .eventDescription(description)
                .build();

        assertEquals(description, record.getEventDescription());
    }

    @Test
    void toAvroObject() {
        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventDescription("Description")
                .eventActorId("actorId")
                .eventSubjectPid("12345")
                .authEid("eid")
                .authMethod("method")
                .correlationId("correlationId")
                .serviceOwnerId("serviceOwnerId")
                .serviceOwnerOrgno("serviceOwnerOrgno")
                .serviceProviderId("serviceProviderId")
                .serviceProviderOrgno("serviceProviderOrgno")
                .extraData(Collections.singletonMap("key", "value"))
                .eventCreated(Instant.now().minus(Duration.ofSeconds(60)))
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .schemaRegistryUrl("test")
                .kafkaUsername("user")
                .activityRecordTopic("activityTopic")
                .applicationName("applicationName")
                .environmentName("applicationEnvironment").build();

        ActivityRecordAvro avroRecord = (ActivityRecordAvro) record.toProducerRecord(config).value();

        assertEquals(record.getEventName(), avroRecord.getEventName());
        assertEquals(record.getEventDescription(), avroRecord.getEventDescription());
        assertEquals(record.getEventActorId(), avroRecord.getEventActorId());
        assertEquals(record.getAuthEid(), avroRecord.getAuthEid());
        assertEquals(record.getAuthMethod(), avroRecord.getAuthMethod());
        assertEquals(record.getServiceOwnerOrgno(), avroRecord.getServiceOwnerOrgno());
        assertEquals(record.getServiceOwnerId(), avroRecord.getServiceOwnerId());
        assertEquals(record.getServiceProviderId(), avroRecord.getServiceProviderId());
        assertEquals(record.getServiceProviderOrgno(), avroRecord.getServiceProviderOrgno());
        assertEquals(record.getEventSubjectPid(), avroRecord.getEventSubjectPid());
        assertEquals(record.getCorrelationId(), avroRecord.getCorrelationId());
        assertEquals(record.getExtraData(), avroRecord.getExtraData());
        assertEquals(record.getEventCreated().toEpochMilli(), avroRecord.getEventCreated().toEpochMilli());

        assertEquals(record.getApplicationEnvironment(), avroRecord.getApplicationEnvironment());
        assertEquals(record.getApplicationName(), avroRecord.getApplicationName());
    }
}