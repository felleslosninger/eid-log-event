package no.idporten.logging.event;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
public class EventLoggingConfig {
    static final String CUSTOM_PRODUCER_PROPERTIES_FILE_PATH = "custom-kafka-producer.properties";
    static final String BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO = "USER_INFO";
    static final String FEATURE_ENABLED_KEY = "digdir.event.logging.feature-enabled";
    static final String APPLICATION_NAME = "application.name";
    static final String EVENT_TOPIC_KEY = "event.topic";
    static final String THREAD_POOL_SIZE_KEY = "thread.pool.size";

    private static final String PRODUCER_PROPERTIES_FILE_PATH = "kafka-producer.properties";
    private static final String EVENT_LOGGER_PROPERTIES_FILE_PATH = "event-logger.properties";
    private static final String JAAS_CONFIG_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule " +
            "required username=\"%s\" password=\"%s\";";
    private static final String NULL_TEMPLATE = "%s must not be null. Please check the configuration.";
    /**
     * Feature toggle
     */
    private final boolean featureEnabled;

    /**
     * Name of the application using this library
     */
    private final String applicationName;

    /**
     * Host and port of the kafka broker(s) <BR>
     * (comma-separated list in the case of multiple servers)
     */
    private final String bootstrapServers;

    /**
     * Host and port of the Schema Registry (Confluent)
     */
    private final String schemaRegistryUrl;

    /**
     * Login for the JAAS SASL configuration
     */
    private final String kafkaUsername;

    /**
     * Password for the JAAS SASL configuration
     */
    private final String kafkaPassword;

    /**
     * Username for the Schema Registry, leave empty for no authentication
     */
    private final String schemaRegistryUsername;

    /**
     * Password for the Schema Registry
     */
    private final String schemaRegistryPassword;

    /**
     * Kafka topic to publish to
     */
    private final String eventTopic;
    /**
     * Number of working threads in the eventLoggerThreadPool.
     */
    private final int threadPoolSize;
    private final Map<String, Object> producerConfig;

    @Builder
    public EventLoggingConfig(
            Boolean featureEnabled,
            String applicationName,
            String bootstrapServers,
            String schemaRegistryUrl,
            String kafkaUsername,
            String kafkaPassword,
            String schemaRegistryUsername,
            String schemaRegistryPassword,
            String eventTopic,
            Integer threadPoolSize) {
        this.kafkaPassword = kafkaPassword;
        this.schemaRegistryUsername = schemaRegistryUsername;
        this.schemaRegistryPassword = schemaRegistryPassword;

        Properties eventLoggerDefaultProperties = loadPropertiesFromFile(EVENT_LOGGER_PROPERTIES_FILE_PATH);
        this.featureEnabled = Optional.ofNullable(featureEnabled).orElse(
                Boolean.valueOf(eventLoggerDefaultProperties.getProperty(FEATURE_ENABLED_KEY, "true"))
        );
        this.threadPoolSize = Optional.ofNullable(threadPoolSize).orElse(
                Integer.valueOf(eventLoggerDefaultProperties.getProperty(THREAD_POOL_SIZE_KEY)));

        if (this.featureEnabled) {
            this.bootstrapServers = Objects.requireNonNull(bootstrapServers, String.format(
                    NULL_TEMPLATE,
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
            this.schemaRegistryUrl = Objects.requireNonNull(schemaRegistryUrl, String.format(
                    NULL_TEMPLATE,
                    KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG));
            this.kafkaUsername = Objects.requireNonNull(kafkaUsername, String.format(
                    NULL_TEMPLATE,
                    KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE));
            this.applicationName = resolveProperty(APPLICATION_NAME, applicationName, eventLoggerDefaultProperties);
            this.eventTopic = resolveProperty(EVENT_TOPIC_KEY, eventTopic, eventLoggerDefaultProperties);
            this.producerConfig = Collections.unmodifiableMap(createProducerConfig());
        } else {
            this.applicationName = null;
            this.bootstrapServers = null;
            this.schemaRegistryUrl = null;
            this.kafkaUsername = null;
            this.eventTopic = "";
            this.producerConfig = null;
        }
    }

    private static Map<String, ?> propertiesToMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())));
    }

    private static String resolveProperty(String key, String userSpecifiedValue, Properties defaultProperties) {
        if (isEmpty(userSpecifiedValue)) {
            return Objects.requireNonNull(defaultProperties.getProperty(key), String.format("No default %s found", key));
        } else {
            return userSpecifiedValue;
        }
    }

    private Map<String, Object> createProducerConfig() {
        Properties kafkaProducerProperties = loadPropertiesFromFile(PRODUCER_PROPERTIES_FILE_PATH);
        kafkaProducerProperties =
                overrideWithOptionalConfig(kafkaProducerProperties, CUSTOM_PRODUCER_PROPERTIES_FILE_PATH);
        Map<String, Object> producerConfig = new HashMap<>(propertiesToMap(kafkaProducerProperties));
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        if (!isEmpty(schemaRegistryUsername)) {
            producerConfig.put(
                    KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
                    BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO);
            producerConfig.put(
                    KafkaAvroSerializerConfig.USER_INFO_CONFIG,
                    String.format("%s:%s", schemaRegistryUsername, defaultIfEmpty(schemaRegistryPassword, "")));
        } else {
            producerConfig.put(
                    KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
                    AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE_DEFAULT);
        }
        producerConfig.put(
                SaslConfigs.SASL_JAAS_CONFIG,
                String.format(JAAS_CONFIG_TEMPLATE, kafkaUsername, kafkaPassword != null ? kafkaPassword : ""));


        return producerConfig;
    }

    Properties overrideWithOptionalConfig(Properties originalProperties, String customConfigFilePath) {
        URL url = getClass().getClassLoader().getResource(customConfigFilePath);
        if (url != null) {
            Properties customProperties = loadPropertiesFromFile(customConfigFilePath);
            originalProperties.putAll(customProperties);
        }
        return originalProperties;
    }

    private Properties loadPropertiesFromFile(String propertiesFilePath) {
        Properties properties = new Properties();

        try {
            InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream(propertiesFilePath);
            properties.load(propertiesStream);
        } catch (Exception e) {
            log.warn("Failed to load properties from {}", propertiesFilePath, e);
        }
        return properties;
    }

    public Map<String, Object> getProducerConfig() {
        return producerConfig;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEventTopic() {
        return eventTopic;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }
}
