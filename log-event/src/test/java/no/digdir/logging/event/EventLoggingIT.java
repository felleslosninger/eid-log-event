package no.digdir.logging.event;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.digdir.logging.event.generated.ActivityRecordAvro;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventLoggingIT {

    private static final String TOPIC = "aktiviteter";
    private static final long TEN_SECONDS = 10_000L;

    private static KafkaContainer kafkaContainer;
    private static GenericContainer<?> schemaRegistryContainer;
    private static String schemaRegistryUrl;

    @BeforeAll
    static void setUp() {
        Network network = Network.newNetwork();

        String kafkaVersion = System.getProperty("confluent.version", "7.9.1"); // set in pom.xml
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka")
                .withTag(kafkaVersion))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");
        kafkaContainer.start();

        schemaRegistryContainer = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry")
                .withTag(kafkaVersion))
                .withNetwork(network)
                .withNetworkAliases("schema-registry")
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
                .withExposedPorts(8081);
        schemaRegistryContainer.start();

        String host = schemaRegistryContainer.getHost();
        Integer port = schemaRegistryContainer.getMappedPort(8081);
        schemaRegistryUrl = "http://" + host + ":" + port;
    }

    @AfterAll
    static void tearDown() {
        if (schemaRegistryContainer != null) {
            schemaRegistryContainer.stop();
        }
        if (kafkaContainer != null) {
            kafkaContainer.stop();
        }
    }

    private static EventLoggingConfig removeSecurityProperty(EventLoggingConfig config) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Object> configWithoutSecurity = config.getProducerConfig().entrySet().stream()
                .filter(e -> !e.getKey().equals("security.protocol"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Field producerConfig = config.getClass().getDeclaredField("producerConfig");
        producerConfig.setAccessible(true);
        producerConfig.set(config, configWithoutSecurity);
        return config;
    }

    @Test
    void shouldLog() throws Exception {
        final List<ActivityRecord> inputValues = List.of(
                ActivityRecord.builder()
                        .eventName("Innlogget")
                        .eventDescription("Brukeren har logget inn")
                        .eventSubjectPid("24079409630")
                        .correlationId(UUID.randomUUID().toString())
                        .serviceProviderId("NAV")
                        .authEid("Buypass")
                        .authMethod("PIN")
                        .build(),
                ActivityRecord.builder()
                        .eventName("Utlogget")
                        .eventDescription("Brukeren har logget ut")
                        .eventSubjectPid("24079409479")
                        .correlationId(UUID.randomUUID().toString())
                        .serviceProviderId("Skatteetaten")
                        .authEid("BankID")
                        .authMethod("PIN")
                        .build(),
                ActivityRecord.builder()
                        .eventName("Endret")
                        .eventDescription("Brukeren har endret passordet sitt")
                        .eventSubjectPid("24079409398")
                        .correlationId(UUID.randomUUID().toString())
                        .serviceProviderId("ID-porten")
                        .authEid("MinID")
                        .authMethod("App")
                        .build()
        );

        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("integrationTest")
                .environmentName("testcontainers")
                .bootstrapServers(kafkaContainer.getBootstrapServers())
                .schemaRegistryUrl(schemaRegistryUrl)
                .kafkaUsername("testuser")
                .kafkaPassword("testpassword")
                .activityRecordTopic(TOPIC)
                .build();

        EventLogger eventLogger = new DefaultEventLogger(removeSecurityProperty(config));

        final Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProperties.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, TOPIC + "-consumer");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        consumerProperties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        try (KafkaConsumer<String, ActivityRecordAvro> consumer = new KafkaConsumer<>(consumerProperties)) {

            consumer.subscribe(Collections.singleton(TOPIC));

            for (ActivityRecord inputValue : inputValues) {
                eventLogger.log(inputValue);
            }

            List<String> expected = inputValues.stream()
                    .map(ActivityRecord::getEventSubjectPid)
                    .toList();
            List<String> received = new ArrayList<>();
            long timeout = System.currentTimeMillis() + TEN_SECONDS;
            while (System.currentTimeMillis() < timeout && !received.containsAll(expected)) {
                ConsumerRecords<String, ActivityRecordAvro> records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(record -> received.add(record.value().getEventSubjectPid().toString()));
            }
            assertTrue(received.containsAll(expected) && expected.containsAll(received));
        }
    }
}
