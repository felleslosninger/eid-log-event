package no.idporten.logging.event;

import io.confluent.examples.streams.kafka.EmbeddedSingleNodeKafkaCluster;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test using the embedded Kafka cluster by Confluent
 */
class EventLoggingIT {

    private static final EmbeddedSingleNodeKafkaCluster CLUSTER = new EmbeddedSingleNodeKafkaCluster();
    private static final String TOPIC = "aktiviteter";
    private static final long TEN_SECONDS = 10_000L;

    @BeforeAll
    static void beforeAll() throws Exception {
        CLUSTER.start();
        CLUSTER.createTopic(TOPIC);
    }

    @AfterAll
    static void afterAll() {
        CLUSTER.stop();
    }

    private static EventLoggingConfig removeSecurityProperty(EventLoggingConfig config) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Object> configWithoutSecurity = config.getProducerConfig().entrySet().stream()
                .filter(e -> !"security.protocol".equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Field producerConfig = config.getClass().getDeclaredField("producerConfig");
        producerConfig.setAccessible(true);
        producerConfig.set(config, configWithoutSecurity);
        return config;
    }

    @Test
    void shouldLog() throws Exception {
        final List<EventRecord> inputValues = Arrays.asList(
                EventRecord.newBuilder()
                        .setName("Innlogget")
                        .setPid("24079409630")
                        .setCorrelationId(UUID.randomUUID().toString())
                        .setService("idPorten")
                        .build(),
                EventRecord.newBuilder()
                        .setName("Utlogget")
                        .setPid("24079409479")
                        .setCorrelationId(UUID.randomUUID().toString())
                        .setService("idPorten")
                        .build(),
                EventRecord.newBuilder()
                        .setName("Endret")
                        .setPid("24079409398")
                        .setCorrelationId(UUID.randomUUID().toString())
                        .setService("idPorten")
                        .build());

        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers(CLUSTER.bootstrapServers())
                .schemaRegistryUrl(CLUSTER.schemaRegistryUrl())
                .kafkaUsername("franz")
                .kafkaPassword("password")
                .eventTopic(TOPIC)
                .build();

        EventLogger eventLogger = new EventLogger(removeSecurityProperty(config));

        final Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers());
        consumerProperties.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, CLUSTER.schemaRegistryUrl());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, TOPIC + "-consumer");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

        List<String> expected;
        List<String> received;
        try (KafkaConsumer<String, EventRecord> consumer = new KafkaConsumer<>(consumerProperties)) {
            consumer.subscribe(Collections.singleton(TOPIC));

            for (EventRecord inputValue : inputValues) {
                eventLogger.log(inputValue);
            }

            expected = inputValues.stream()
                    .map(record -> record.getPid().toString())
                    .collect(Collectors.toList());
            received = new ArrayList<>();
            long timeout = System.currentTimeMillis() + TEN_SECONDS;
            while (System.currentTimeMillis() < timeout && !received.containsAll(expected)) {
                ConsumerRecords<String, EventRecord> records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(record -> received.add(record.key()));
            }
        }

        assertTrue(received.containsAll(expected) & expected.containsAll(received), "Not all the expected messages was received by the consumer");
    }


}
