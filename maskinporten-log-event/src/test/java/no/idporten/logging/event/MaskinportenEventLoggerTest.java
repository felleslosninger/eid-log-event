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

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaskinportenEventLoggerTest {
    private static final String DUMMY_URL = "https://localhost:443";
    private static final String USERNAME = "username";
    private static final String APPLICATION_NAME = "testApplication";
    private static final String ENVIRONMENT_NAME = "unitTest";
    private static final String CONSUMER = "0192:999888777";
    private static final String TOPIC = "mpAktivitet";
    private static final int POOL_SIZE = 1;

    private final EventLoggingConfig config = EventLoggingConfig.builder()
            .applicationName(APPLICATION_NAME)
            .environmentName(ENVIRONMENT_NAME)
            .bootstrapServers(DUMMY_URL)
            .schemaRegistryUrl(DUMMY_URL)
            .kafkaUsername(USERNAME)
            .threadPoolSize(POOL_SIZE)
            .eventTopic(TOPIC)
            .build();

    private final MaskinportenEventRecord record = MaskinportenEventRecord.newBuilder()
            .setName("Token utstedt")
            .setCorrelationId(UUID.randomUUID().toString())
            .setIss("https://maskinporten.no/")
            .setClientId("test client ID")
            .setClientAmr("virksomhetssertifikat")
            .setConsumer(CONSUMER)
            .setSupplier("0192:777888999")
            .setScope("folkeregister:offentlig_med_hjemmel")
            .build();

    private MaskinportenEventLogger eventLogger;

    @BeforeEach
    void setUp() {
        MockSchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
        SpecificAvroSerde<MaskinportenEventRecord> serde = new SpecificAvroSerde<>(schemaRegistryClient);
        serde.configure(config.getProducerConfig(), false);

        eventLogger = new MaskinportenEventLogger(config);
        eventLogger.producer.close();
        eventLogger.producer = new MockProducer<>(true, new StringSerializer(), serde.serializer());
    }

    @Test
    void log() throws ExecutionException, InterruptedException {
        eventLogger.log(record);
        eventLogger.producer.flush();
        MockProducer<String, MaskinportenEventRecord> mockProducer = (MockProducer<String, MaskinportenEventRecord>) eventLogger.producer;
        Future<Integer> sentEventsFuture = eventLogger.pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsFuture.get(), "Record should be published");
        assertEquals(CONSUMER, mockProducer.history().get(0).key(), "Record key should be the Consumer");
        assertEquals(TOPIC, mockProducer.history().get(0).topic(), "Topic should be Maskinporten-specific");
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

        eventLogger = new MaskinportenEventLogger(disablingConfig);
        eventLogger.log(record);
        eventLogger.producer.flush();
        assertTrue(eventLogger.producer instanceof NoLoggingProducer, "Logger should be non-logging");
    }

    @SuppressWarnings("unchecked")
    @Test
    void logWhenFailure() {
        Producer<String, MaskinportenEventRecord> producerMock = mock(Producer.class);
        when(producerMock.send(any(ProducerRecord.class)))
                .thenThrow(new KafkaException("Simulating Kafka down"));
        eventLogger.producer = producerMock;

        eventLogger.log(record);
    }

}