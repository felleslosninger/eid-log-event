package no.digdir.logging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class EventLoggingIT {

    private static final String TOPIC = "aktiviteter";
    private static final long TEN_SECONDS = 10_000L;

    private static KafkaContainer kafkaContainer;
    private static final ObjectMapper objectMapper = new ObjectMapper();


    @BeforeAll
    static void setUp() {
        Network network = Network.newNetwork();

        String kafkaVersion = System.getProperty("kafka.version", "4.0.0"); // set in pom.xml
        kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka")
                .withTag(kafkaVersion))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");
        kafkaContainer.start();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterAll
    static void tearDown() {
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
                .kafkaUsername("admin")
                .kafkaPassword("password123")
                .activityRecordTopic(TOPIC)
                .build();

        EventLogger eventLogger = new DefaultEventLogger(removeSecurityProperty(config));

        final Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, TOPIC + "-consumer");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {

            consumer.subscribe(Collections.singleton(TOPIC));

            for (ActivityRecord inputValue : inputValues) {
                eventLogger.log(inputValue);
                log.info("Sent to kafka {}", inputValue);
            }

            List<String> expected = inputValues.stream()
                    .map(ActivityRecord::getEventSubjectPid)
                    .toList();

            List<String> received = new ArrayList<>();
            long timeout = System.currentTimeMillis() + TEN_SECONDS;
            while (System.currentTimeMillis() < timeout && !received.containsAll(expected)) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(record -> {
                    try {
                        received.add(extractEventSubjectPidFromJson(record.value()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            assertTrue(received.containsAll(expected) && expected.containsAll(received));
            assertEquals(3, received.size());
            assertEquals(3, expected.size());
        }
    }

    private String extractEventSubjectPidFromJson(String inputJson) throws JsonProcessingException {
        var jsonNode = parseJson(inputJson);
        if (jsonNode.has("event_subject_pid")) {
            return jsonNode.get("event_subject_pid").asText();
        }
        throw new IllegalArgumentException("No event_subject_pid found in json: " + inputJson);
    }

    private JsonNode parseJson(String inputJson) throws JsonProcessingException {
        // Parse the JSON string to a JsonNode for easier property access
        return objectMapper.readTree(inputJson);
    }
}
