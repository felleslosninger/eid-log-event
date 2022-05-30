package no.digdir.logging.event;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventLoggerTest {

    private static final String DUMMY_URL = "https://localhost:443";
    private static final String USERNAME = "username";
    private static final String FNR = "25079494081";
    private static final String APPLICATION_NAME = "testApplication";
    private static final String ENVIRONMENT_NAME = "unitTest";
    private static final int POOL_SIZE = 1;

    private final EventLoggingConfig config = EventLoggingConfig.builder()
            .applicationName(APPLICATION_NAME)
            .environmentName(ENVIRONMENT_NAME)
            .bootstrapServers(DUMMY_URL)
            .schemaRegistryUrl(DUMMY_URL)
            .kafkaUsername(USERNAME)
            .threadPoolSize(POOL_SIZE)
            .build();

    private final ActivityRecord record = ActivityRecord.newBuilder()
            .setEventName("Innlogget")
            .setEventDescription("Brukeren har logget inn")
            .setEventSubjectPid(FNR)
            .setCorrelationId(UUID.randomUUID().toString())
            .setServiceProviderId("McDuck IT")
            .setServiceOwnerId("Andeby kommune")
            .setAuthEid("MinID")
            .setAuthMethod("OTC")
            .build();

    private EventLogger eventLogger;

    @BeforeEach
    void setUp() {
        MockSchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
        SpecificAvroSerde<ActivityRecord> serde = new SpecificAvroSerde<>(schemaRegistryClient);
        serde.configure(config.getProducerConfig(), false);

        eventLogger = new EventLogger(config);
        eventLogger.activityProducer.close();
        eventLogger.activityProducer = new MockProducer<>(true, new StringSerializer(), serde.serializer());
    }

    @Test
    void log() throws ExecutionException, InterruptedException {
        eventLogger.log(record);
        eventLogger.activityProducer.flush();
        MockProducer<String, ActivityRecord> mockProducer = (MockProducer<String, ActivityRecord>) eventLogger.activityProducer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        assertNull(mockProducer.history().get(0).key(), "Record key should be null");
    }

    @Test
    void logMaskinportenAuthentication() throws ExecutionException, InterruptedException {
        MaskinportenAuthentication record = MaskinportenAuthentication.newBuilder()
                .setEventName("Innlogget")
                .setEventDescription("Brukeren har logget inn")
                .setCorrelationId(UUID.randomUUID().toString())
                .setClientId("Test client ID")
                .setClientOrgno("987654321")
                .setClientOnBehalfOfId("Andeby kommune")
                .build();

        eventLogger.log(record);
        eventLogger.mpAuthenticationProducer.flush();
        MockProducer<String, MaskinportenAuthentication> mockProducer = (MockProducer<String, MaskinportenAuthentication>) eventLogger.mpAuthenticationProducer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        assertNull(mockProducer.history().get(0).key(), "Record key should be null");
    }

    @SuppressWarnings("unchecked")
    @Test
    void logWhenFailure() {
        Producer<String, ActivityRecord> producerMock = mock(Producer.class);
        when(producerMock.send(any(ProducerRecord.class)))
                .thenThrow(new KafkaException("Simulating Kafka down"));
        eventLogger.activityProducer = producerMock;

        eventLogger.log(record);
    }

    @Test
    void noLoggingWhenDisabled() {
        EventLoggingConfig disablingConfig = EventLoggingConfig.builder()
                .applicationName(APPLICATION_NAME)
                .environmentName(ENVIRONMENT_NAME)
                .bootstrapServers(DUMMY_URL)
                .schemaRegistryUrl(DUMMY_URL)
                .kafkaUsername(USERNAME)
                .eventTopic("any topic")
                .featureEnabled(false)
                .build();

        eventLogger = new EventLogger(disablingConfig);
        eventLogger.log(record);
        eventLogger.activityProducer.flush();
        assertTrue(eventLogger.activityProducer instanceof NoLoggingProducer, "Logger should be non-logging");
    }

    @Test
    void noConfigWhenDisabled() {
        EventLoggingConfig disablingConfig = EventLoggingConfig.builder()
                .featureEnabled(false)
                .build();

        eventLogger = new EventLogger(disablingConfig);
        eventLogger.log(record);
        eventLogger.activityProducer.flush();
        assertTrue(eventLogger.activityProducer instanceof NoLoggingProducer, "Logger should be non-logging");
    }

    @Test
    void defaultCreationTimestamp() throws ExecutionException, InterruptedException {
        assertNull(record.getEventCreatedMs(), "Test is void: created time set directly on record");

        eventLogger.log(record);
        eventLogger.activityProducer.flush();
        MockProducer<String, ActivityRecord> mockProducer = (MockProducer<String, ActivityRecord>) eventLogger.activityProducer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        ActivityRecord loggedRecord = mockProducer.history().get(0).value();

        assertTrue(Instant.now().isAfter(loggedRecord.getEventCreatedMs()),
                "Creation timestamp should be earlier than current time");
    }

    @Test
    void defaultApplicationName() throws ExecutionException, InterruptedException {
        assertNull(record.getApplicationName(), "Test is void: application name set directly on record");

        eventLogger.log(record);
        eventLogger.activityProducer.flush();
        MockProducer<String, ActivityRecord> mockProducer = (MockProducer<String, ActivityRecord>) eventLogger.activityProducer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        ActivityRecord loggedRecord = mockProducer.history().get(0).value();
        assertEquals(APPLICATION_NAME, loggedRecord.getApplicationName(), "Application name not set from config");
    }

    @Test
    void defaultEnvironmentName() throws ExecutionException, InterruptedException {
        assertNull(record.getApplicationName(), "Test is void: environment name set directly on record");

        eventLogger.log(record);
        eventLogger.activityProducer.flush();
        MockProducer<String, ActivityRecord> mockProducer = (MockProducer<String, ActivityRecord>) eventLogger.activityProducer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        ActivityRecord loggedRecord = mockProducer.history().get(0).value();
        assertEquals(ENVIRONMENT_NAME, loggedRecord.getApplicationEnvironment(), "Environment name not set from config");
    }

    @Test
    void silentFailing() {
        eventLogger.finalize(); // producer is closed
        eventLogger.log(record);
    }

    @Test
    void threadPoolSize() {
        assertTrue(eventLogger.pool instanceof ThreadPoolExecutor, "The threadPool should be of type ThreadPoolExecutor");
        assertEquals(POOL_SIZE, ((ThreadPoolExecutor) eventLogger.pool).getCorePoolSize(),
                "PoolSize should have been initialized to " + POOL_SIZE);
        EventLoggingConfig customPoolSizeConfig = EventLoggingConfig.builder()
                .applicationName(APPLICATION_NAME)
                .environmentName(ENVIRONMENT_NAME)
                .bootstrapServers(DUMMY_URL)
                .schemaRegistryUrl(DUMMY_URL)
                .kafkaUsername(USERNAME)
                .eventTopic("any topic")
                .featureEnabled(true)
                .threadPoolSize(20)
                .build();

        eventLogger = new EventLogger(customPoolSizeConfig);
        assertTrue(eventLogger.pool instanceof ThreadPoolExecutor, "The threadPool should still be of type ThreadPoolExecutor");
        assertEquals(20, ((ThreadPoolExecutor) eventLogger.pool).getCorePoolSize(), "poolSize should be equal to the new custom set size");
    }

    @Test
    void queueStats() {
        assertTrue(eventLogger.getPoolQueueStats().contains("ThreadPoolSize:"),
                "\"ThreadPoolSize:\" is the first part of the queueStats when the stats are displayed as they should.");
    }

    @Test
    void metrics() {
        assertNotNull(eventLogger.getMetrics(), "The eventLoggers metrics should be reachable and available.");
    }
}