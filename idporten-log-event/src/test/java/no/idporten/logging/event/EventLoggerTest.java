package no.idporten.logging.event;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import no.idporten.logging.event.config.EventLoggingConfig;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventLoggerTest {

    private static final String DUMMY_URL = "example.com:80";
    private static final String USERNAME = "username";
    EventLogger eventLogger;

    @BeforeEach
    void setUp() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers(DUMMY_URL)
                .schemaRegistryUrl(DUMMY_URL)
                .username(USERNAME)
                .build();

        MockSchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
        SpecificAvroSerde<EventRecord> serde = new SpecificAvroSerde<>(schemaRegistryClient);
        serde.configure(config.toMap(), false);

        eventLogger = new EventLogger(config);
        eventLogger.producer = new MockProducer<>(true, new StringSerializer(), serde.serializer());
    }

    @Test
    void log() {
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid("25079494081")
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        eventLogger.log(record);

        if (ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.SECONDS)) { // wait for async completion
            MockProducer<String, EventRecord> mockProducer = (MockProducer<String, EventRecord>) eventLogger.producer;
            assertEquals(1, mockProducer.history().size(), "Record should be published");
        }
    }

    @Test
    void logWhenFailure() {
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid("25079494081")
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        Producer<String, EventRecord> producerMock = mock(Producer.class);
        when(producerMock.send(any(ProducerRecord.class)))
                .thenThrow(new KafkaException("Kafka down"));
        eventLogger.producer = producerMock;

        eventLogger.log(record);
    }

    @AfterEach
    void tearDown() {
        eventLogger.finalize();
    }
}