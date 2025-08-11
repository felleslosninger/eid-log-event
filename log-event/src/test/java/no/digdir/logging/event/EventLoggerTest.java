package no.digdir.logging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

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
            .kafkaUsername(USERNAME)
            .threadPoolSize(POOL_SIZE)
            .build();

    private final ActivityRecord record = ActivityRecord.builder()
            .eventName("Innlogget")
            .eventDescription("Brukeren har logget inn")
            .eventSubjectPid(FNR)
            .correlationId(UUID.randomUUID().toString())
            .serviceProviderId("McDuck IT")
            .serviceOwnerId("Andeby kommune")
            .authEid("MinID")
            .authMethod("OTC")
            .build();
    private final Partitioner partitioner = new Partitioner() {

        @Override
        public int partition(
                String topic,
                Object key,
                byte[] keyBytes,
                Object value,
                byte[] valueBytes,
                Cluster cluster) {
            return 0; // Always assign to partition 0
        }

        @Override
        public void close() {
        }

        @Override
        public void configure(java.util.Map<String, ?> configs) {
        }
    };
    private DefaultEventLogger eventLogger;
    private MockProducer<String, String> kafkaProducer;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        Serde<String> serde = new Serdes.StringSerde();
        serde.configure(config.getProducerConfig(), false);

        kafkaProducer = spy(new MockProducer<>(true, partitioner, new StringSerializer(), serde.serializer()));
        executorService = new EventLoggerThreadPoolExecutor(config);
        eventLogger = new DefaultEventLogger(config, kafkaProducer, executorService);
    }

    @Test
    void log() throws ExecutionException, InterruptedException {
        eventLogger.log(record);
        kafkaProducer.flush();
        Future<Integer> sentEventsFuture = executorService.submit(() -> kafkaProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        assertNull(kafkaProducer.history().get(0).key(), "Record key should be null");
    }

    @Test
    void logMaskinportenAuthentication() throws ExecutionException, InterruptedException {
        MPAuthenticationRecord record = MPAuthenticationRecord.builder()
                .eventName("Innlogget")
                .eventDescription("Brukeren har logget inn")
                .correlationId(UUID.randomUUID().toString())
                .clientId("Test client ID")
                .clientOrgno("987654321")
                .clientOnBehalfOfId("Andeby kommune")
                .build();

        eventLogger.log(record);
        kafkaProducer.flush();
        Future<Integer> sentEventsFuture = executorService.submit(() -> kafkaProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        assertNull(kafkaProducer.history().get(0).key(), "Record key should be null");
    }

    @Test
    void logWhenFailure() {
        doThrow(new KafkaException("Simulating Kafka down")).when(kafkaProducer).send(any(), any(Callback.class));
        assertDoesNotThrow(() -> eventLogger.log(record));
    }

    @Test
    void defaultCreationTimestamp() throws ExecutionException, InterruptedException, JsonProcessingException {
        eventLogger.log(record);
        kafkaProducer.flush();
        Future<Integer> sentEventsFuture = executorService.submit(() -> kafkaProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        String jsonRecord = kafkaProducer.history().get(0).value();
        // Parse the JSON string to a JsonNode for easier property access
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonRecord);

        assertTrue(Instant.now().isAfter(Instant.ofEpochMilli(jsonNode.get("event_created").asLong())),
                "Creation timestamp should be earlier than current time");
    }

    @Test
    void defaultApplicationName() throws ExecutionException, InterruptedException, JsonProcessingException {
        assertNull(record.getApplicationName(), "Test is void: application name set directly on record");

        eventLogger.log(record);
        kafkaProducer.flush();
        Future<Integer> sentEventsFuture = executorService.submit(() -> kafkaProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        String jsonRecord = kafkaProducer.history().get(0).value();
        // Parse the JSON string to a JsonNode for easier property access
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonRecord);
        assertEquals(APPLICATION_NAME, jsonNode.get("application_name")
                .asText(), "Application name not set from config");
    }

    @Test
    void defaultEnvironmentName() throws ExecutionException, InterruptedException, JsonProcessingException {
        assertNull(record.getApplicationName(), "Test is void: environment name set directly on record");

        eventLogger.log(record);
        kafkaProducer.flush();
        Future<Integer> sentEventsFuture = executorService.submit(() -> kafkaProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        String jsonRecord = kafkaProducer.history().get(0).value();
        // Parse the JSON string to a JsonNode for easier property access
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonRecord);
        assertEquals(ENVIRONMENT_NAME, jsonNode.get("application_environment")
                .asText(), "Environment name not set from config");
    }

    @Test
    void silentFailing() {
        eventLogger.preDestroy(); // producer is closed
        assertDoesNotThrow(() -> eventLogger.log(record));
    }

    @Test
    void threadPoolThreadSize() {
        assertInstanceOf(ThreadPoolExecutor.class, executorService, "The threadPool should be of type ThreadPoolExecutor");
        assertEquals(POOL_SIZE, ((ThreadPoolExecutor) executorService).getCorePoolSize(),
                "PoolSize should have been initialized to " + POOL_SIZE);
        EventLoggingConfig customPoolSizeConfig = EventLoggingConfig.builder()
                .applicationName(APPLICATION_NAME)
                .environmentName(ENVIRONMENT_NAME)
                .bootstrapServers(DUMMY_URL)
                .kafkaUsername(USERNAME)
                .activityRecordTopic("any topic")
                .featureEnabled(true)
                .threadPoolSize(20)
                .build();

        var customEventLogger = new DefaultEventLogger(customPoolSizeConfig);
        assertInstanceOf(ThreadPoolExecutor.class, customEventLogger.getExecutorService(), "The threadPool should still be of type ThreadPoolExecutor");
        assertEquals(20, ((ThreadPoolExecutor) customEventLogger.getExecutorService()).getCorePoolSize(), "poolSize should be equal to the new custom set size");
    }

    @Test
    void threadPoolQueueSize() {
        EventLoggingConfig customPoolSizeConfig = EventLoggingConfig.builder()
                .applicationName(APPLICATION_NAME)
                .environmentName(ENVIRONMENT_NAME)
                .bootstrapServers(DUMMY_URL)
                .kafkaUsername(USERNAME)
                .activityRecordTopic("any topic")
                .featureEnabled(true)
                .threadPoolQueueSize(200)
                .build();

        var customEventLogger = new DefaultEventLogger(customPoolSizeConfig);
        assertInstanceOf(ThreadPoolExecutor.class, customEventLogger.getExecutorService(), "The threadPool should be of type ThreadPoolExecutor");
        assertEquals(200, ((ThreadPoolExecutor) customEventLogger.getExecutorService()).getQueue()
                .remainingCapacity(), "poolSize remaining capacity should be equal to max capacity since its not in use yet");
    }

    @Test
    void metrics() {
        assertNotNull(eventLogger.getMetrics(), "The eventLoggers metrics should be reachable and available.");
    }
}