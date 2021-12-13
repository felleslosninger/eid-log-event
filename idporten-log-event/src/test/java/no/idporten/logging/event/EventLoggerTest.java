package no.idporten.logging.event;

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
    private final EventRecord record = EventRecord.newBuilder()
            .setName("Innlogget")
            .setDescription("Brukeren har logget inn")
            .setPid(FNR)
            .setCorrelationId(UUID.randomUUID().toString())
            .setService("idPorten")
            .build();
    private EventLogger eventLogger;

    @BeforeEach
    void setUp() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName(APPLICATION_NAME)
                .environmentName(ENVIRONMENT_NAME)
                .bootstrapServers(DUMMY_URL)
                .schemaRegistryUrl(DUMMY_URL)
                .kafkaUsername(USERNAME)
                .build();

        MockSchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
        SpecificAvroSerde<EventRecord> serde = new SpecificAvroSerde<>(schemaRegistryClient);
        serde.configure(config.getProducerConfig(), false);

        eventLogger = new EventLogger(config);
        eventLogger.producer.close();
        eventLogger.producer = new MockProducer<>(true, new StringSerializer(), serde.serializer());
    }

    @Test
    void log() throws ExecutionException, InterruptedException {
        eventLogger.log(record);
        eventLogger.producer.flush();
        MockProducer<String, EventRecord> mockProducer = (MockProducer<String, EventRecord>) eventLogger.producer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        assertEquals(FNR, mockProducer.history().get(0).key(), "Record key should be the PID");
    }

    @SuppressWarnings("unchecked")
    @Test
    void logWhenFailure() {
        Producer<String, EventRecord> producerMock = mock(Producer.class);
        when(producerMock.send(any(ProducerRecord.class)))
                .thenThrow(new KafkaException("Simulating Kafka down"));
        eventLogger.producer = producerMock;

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
        eventLogger.producer.flush();
        assertTrue(eventLogger.producer instanceof NoLoggingProducer, "Logger should be non-logging");
    }

    @Test
    void noConfigWhenDisabled() {
        EventLoggingConfig disablingConfig = EventLoggingConfig.builder()
                .featureEnabled(false)
                .build();

        eventLogger = new EventLogger(disablingConfig);
        eventLogger.log(record);
        eventLogger.producer.flush();
        assertTrue(eventLogger.producer instanceof NoLoggingProducer, "Logger should be non-logging");
    }

    @Test
    void defaultCreationTimestamp() {
        Instant now = Instant.now();
        assertEquals(null, record.getCreated(), "Unexpected initialization of record creation time");

        eventLogger.log(record);
        assertTrue((now.compareTo(record.getCreated()) == 0) ||
                        (now.isBefore(record.getCreated()) && now.plusMillis(100).isAfter(record.getCreated())),
                "Creation timestamp not close enough to current time");
    }

    @Test
    void threadPoolSize() {
        assertTrue(eventLogger.pool instanceof ThreadPoolExecutor, "The threadPool should be of type ThreadPoolExecutor");
        assertEquals(4, ((ThreadPoolExecutor) eventLogger.pool).getCorePoolSize(), "Default poolSize should be 4");
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