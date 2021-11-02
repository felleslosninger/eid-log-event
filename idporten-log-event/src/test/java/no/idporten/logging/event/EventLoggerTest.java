package no.idporten.logging.event;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import no.idporten.logging.event.config.EventLoggingConfig;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventLoggerTest {

    private static final String DUMMY_URL = "https://localhost:443";
    private static final String USERNAME = "username";
    private static final String FNR = "25079494081";
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private EventLogger eventLogger;

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

        eventLogger = new EventLogger(config, pool);
        eventLogger.producer.close();
        eventLogger.producer = new MockProducer<>(true, new StringSerializer(), serde.serializer());
    }

    @Test
    void log() throws ExecutionException, InterruptedException {
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        eventLogger.log(record);
        eventLogger.producer.flush();
        MockProducer<String, EventRecord> mockProducer = (MockProducer<String, EventRecord>) eventLogger.producer;
        Future<Integer> sentEventsfuture = pool.submit(() -> mockProducer.history().size());

        assertEquals(1, sentEventsfuture.get(), "Record should be published");
        assertEquals(FNR, mockProducer.history().get(0).key(), "Record key should be the PID");
    }

    @Test
    void logWhenFailure() {
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        Producer<String, EventRecord> producerMock = mock(Producer.class);
        when(producerMock.send(any(ProducerRecord.class)))
                .thenThrow(new KafkaException("Simulating Kafka down"));
        eventLogger.producer = producerMock;

        eventLogger.log(record);
    }

}