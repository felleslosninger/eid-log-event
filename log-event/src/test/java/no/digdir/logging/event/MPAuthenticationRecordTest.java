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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

class MPAuthenticationRecordTest {

    private final EventLoggingConfig defaultConfig = EventLoggingConfig.builder()
            .applicationName("bla")
            .bootstrapServers("test")
            .environmentName("test")
            .build();

    private MockProducer<String, String> kafkaProducer;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        kafkaProducer = spy(new MockProducer<>(true, new MockPartitioner(), new StringSerializer(), new StringSerializer()));
        executorService = new EventLoggerThreadPoolExecutor(defaultConfig);
    }

    @Test
    void topicFromCorrectConfig() throws JsonProcessingException {
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
                .consumer("consumer")
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("192.168.1.1:443")
                .kafkaUsername("user")
                .applicationName("application")
                .environmentName("environment")
                .activityRecordTopic("wrongTopic")
                .maskinportenAuthenticationRecordTopic(topicName)
                .maskinportenTokenRecordTopic("wrongTopic")
                .build();

        DefaultEventLogger eventLogger = new DefaultEventLogger(config, kafkaProducer, executorService);
        ProducerRecord<String, String> result = eventLogger.toProducerRecord(record);
        assertEquals(topicName, result.topic());
    }

    @Test
    void toAvroObject() throws JsonProcessingException {
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
                .consumer("consumer")
                .build();

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers("192.168.1.1:443")
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
        assertEquals(record.getClientId(), jsonNode.get("client_id").asText());
        assertEquals(record.getClientOrgno(), jsonNode.get("client_orgno").asText());
        assertEquals(record.getClientOnBehalfOfId(), jsonNode.get("client_on_behalf_of_id").asText());
        assertEquals(record.getCertificateIssuer(), jsonNode.get("certificate_issuer").asText());
        assertEquals(record.getCertificateSerialNumber(), jsonNode.get("certificate_serial_number").asText());
        assertEquals(record.getKid(), jsonNode.get("kid").asText());
        assertEquals(record.getAud(), jsonNode.get("aud").asText());
        assertEquals(record.getTokenEndpointAuthMethod(), jsonNode.get("token_endpoint_auth_method").asText());
        assertEquals(record.getConsumer(), jsonNode.get("consumer").asText());
        assertEquals(record.getCorrelationId(), jsonNode.get("correlation_id").asText());
        assertEquals(record.getExtraData(), objectMapper.convertValue(jsonNode.get("extra_data"), Map.class));
        assertEquals(record.getEventCreated().toEpochMilli(), jsonNode.get("event_created").asLong());

        assertEquals(record.getApplicationEnvironment(), jsonNode.get("application_environment").asText());
        assertEquals(record.getApplicationName(), jsonNode.get("application_name").asText());
    }
}