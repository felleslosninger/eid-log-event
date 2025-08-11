package no.digdir.logging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.test.MockPartitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

class ActivityRecordTest {

    private static final String FNR = "25079418415";

    private MockProducer<String, String> kafkaProducer;
    private ExecutorService executorService;

    private final EventLoggingConfig defaultConfig = EventLoggingConfig.builder()
            .applicationName("bla")
            .bootstrapServers("test")
            .environmentName("test")
            .build();

    @BeforeEach
    void setUp() {
        kafkaProducer = spy(new MockProducer<>(true, new MockPartitioner(), new StringSerializer(), new StringSerializer()));
        executorService = new EventLoggerThreadPoolExecutor(defaultConfig);
    }

    @Test
    void applicationNameAlwaysFromConfig() throws JsonProcessingException {
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
                .kafkaUsername("user")
                .activityRecordTopic(topicName)
                .applicationName(applicationName)
                .environmentName(applicationEnvironment).build();
        DefaultEventLogger eventLogger = new DefaultEventLogger(config, kafkaProducer, executorService);

        String jsonRecord = eventLogger.toProducerRecord(record).value();

        // Parse the JSON string to a JsonNode for easier property access
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonRecord);

        assertEquals(applicationName, jsonNode.get("application_name").asText());
    }

    @Test
    void applicationEnvironmentAlwaysFromConfig() throws JsonProcessingException {
        String applicationEnvironment = "PROD";

        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid(FNR)
                .correlationId(UUID.randomUUID().toString())
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .kafkaUsername("user")
                .applicationName("application")
                .environmentName(applicationEnvironment).build();

        DefaultEventLogger eventLogger = new DefaultEventLogger(config, kafkaProducer, executorService);
        String jsonRecord = eventLogger.toProducerRecord(record).value();

        // Parse the JSON string to a JsonNode for easier property access
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonRecord);

        assertEquals(applicationEnvironment, jsonNode.get("application_environment").asText());
    }

    @Test
    void topicFromCorrectConfig() throws JsonProcessingException {
        String topicName = "activityRecordTopic";

        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid(FNR)
                .correlationId(UUID.randomUUID().toString())
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .kafkaUsername("user")
                .applicationName("applicationName")
                .environmentName("test")
                .activityRecordTopic(topicName)
                .maskinportenAuthenticationRecordTopic("wrongTopic")
                .maskinportenTokenRecordTopic("wrongTopic")
                .build();

        DefaultEventLogger eventLogger = new DefaultEventLogger(config, kafkaProducer, executorService);
        ProducerRecord<String, String> result = eventLogger.toProducerRecord(record);
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
    void toAvroObject() throws JsonProcessingException {
        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventDescription("Description")
                .eventActorId("actorId")
                .eventSubjectPid(FNR)
                .authEid("eid")
                .authMethod("method")
                .correlationId("correlationId")
                .serviceOwnerId("serviceOwnerId")
                .serviceOwnerOrgno("serviceOwnerOrgno")
                .serviceOwnerName("serviceOwnerName")
                .serviceProviderId("serviceProviderId")
                .serviceProviderOrgno("serviceProviderOrgno")
                .serviceProviderName("serviceProviderName")
                .extraData(Collections.singletonMap("key", "value"))
                .eventCreated(Instant.now().minus(Duration.ofSeconds(60)))
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("test")
                .kafkaUsername("user")
                .activityRecordTopic("activityTopic")
                .applicationName("applicationName")
                .environmentName("applicationEnvironment").build();

        DefaultEventLogger eventLogger = new DefaultEventLogger(config, kafkaProducer, executorService);
        String jsonRecord = eventLogger.toProducerRecord(record).value();

        // Parse the JSON string to a JsonNode for easier property access
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonRecord);

        // Assert equality between the fields from the record and the corresponding JSON properties
        assertEquals(record.getEventName(), jsonNode.get("event_name").asText());
        assertEquals(record.getEventDescription(), jsonNode.get("event_description").asText());
        assertEquals(record.getEventActorId(), jsonNode.get("event_actor_id").asText());
        assertEquals(record.getAuthEid(), jsonNode.get("auth_eid").asText());
        assertEquals(record.getAuthMethod(), jsonNode.get("auth_method").asText());
        assertEquals(record.getServiceOwnerOrgno(), jsonNode.get("service_owner_orgno").asText());
        assertEquals(record.getServiceOwnerName(), jsonNode.get("service_owner_name").asText());
        assertEquals(record.getServiceOwnerId(), jsonNode.get("service_owner_id").asText());
        assertEquals(record.getServiceProviderId(), jsonNode.get("service_provider_id").asText());
        assertEquals(record.getServiceProviderOrgno(), jsonNode.get("service_provider_orgno").asText());
        assertEquals(record.getServiceProviderName(), jsonNode.get("service_provider_name").asText());
        assertEquals(record.getEventSubjectPid(), jsonNode.get("event_subject_pid").asText());
        assertEquals(record.getCorrelationId(), jsonNode.get("correlation_id").asText());
        assertEquals(record.getExtraData(), objectMapper.convertValue(jsonNode.get("extra_data"), Map.class));
        assertEquals(record.getEventCreated().toEpochMilli(), jsonNode.get("event_created").asLong());

        // Derived fields
        assertEquals(1994, jsonNode.get("subject_birthyear").asInt());
        assertEquals(
                Period.between(
                        DateUtil.computeDOB(record.getEventSubjectPid()).get(),
                        record.getEventCreated().atZone(ZoneId.systemDefault()).toLocalDate()
                ).getYears(),
                jsonNode.get("subject_age_at_event").asInt()
        );

        // Application metadata
        assertEquals(record.getApplicationEnvironment(), jsonNode.get("application_environment").asText());
        assertEquals(record.getApplicationName(), jsonNode.get("application_name").asText());
    }

}