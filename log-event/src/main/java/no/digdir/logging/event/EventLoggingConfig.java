package no.digdir.logging.event;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class EventLoggingConfig {
    static final String CUSTOM_PRODUCER_PROPERTIES_FILE_PATH = "custom-kafka-producer.properties";
    static final String BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO = "USER_INFO";
    static final String FEATURE_ENABLED_KEY = "digdir.event.logging.feature-enabled";
    static final String APPLICATION_NAME = "application.name";
    static final String ENVIRONMENT_NAME = "environment.name";
    static final String ACTIVITY_RECORD_TOPIC_KEY = "activity-record.topic";
    static final String MP_AUTH_RECORD_TOPIC_KEY = "maskinporten-authenticated-record.topic";
    static final String MP_TOKEN_RECORD_TOPIC_KEY = "maskinporten-token-record.topic";
    static final String THREAD_POOL_SIZE_KEY = "thread.pool.size";
    static final String THREAD_POOL_QUEUE_SIZE_KEY = "thread.pool.queue.size";
    private static final String PRODUCER_PROPERTIES_FILE_PATH = "kafka-producer.properties";
    private static final String EVENT_LOGGER_PROPERTIES_FILE_PATH = "event-logger.properties";
    private static final String JAAS_CONFIG_TEMPLATE = "org.apache.kafka.common.security.scram.ScramLoginModule " +
            "required username=\"%s\" password=\"%s\";";
    private static final String NULL_TEMPLATE = "%s must not be null. Please check the configuration.";
    /**
     * Feature toggle
     */
    @Getter
    private final boolean featureEnabled;

    /**
     * Name of the application using this library
     */
    @Getter
    private final String applicationName;

    /**
     * Name of the environment where the application is running
     */
    @Getter
    private final String environmentName;

    /**
     * Host and port of the kafka broker(s) <BR>
     * (comma-separated list in the case of multiple servers)
     */
    private final String bootstrapServers;

    /**
     * Login for the JAAS SASL configuration
     */
    private final String kafkaUsername;

    /**
     * Password for the JAAS SASL configuration
     */
    private final String kafkaPassword;

    /**
     * Kafka topic to publish activityRecords to
     */
    @Getter
    private final String activityRecordTopic;
    /**
     * Kafka topic to publish maskinportenAuthenticatedRecords to
     */
    @Getter
    private final String maskinportenAuthenticationRecordTopic;
    /**
     * Kafka topic to publish maskinportenTokenRecords to
     */
    @Getter
    private final String maskinportenTokenRecordTopic;
    /**
     * Number of working threads in the eventLoggerThreadPool.
     */
    @Getter
    private final int threadPoolSize;
    /**
     * The queue-capacity of the queue in front of the threadPool
     */
    @Getter
    private final int threadPoolQueueSize;
    @Getter
    private final Map<String, Object> producerConfig;

    @Builder
    public EventLoggingConfig(
            Boolean featureEnabled,
            String applicationName,
            String environmentName,
            String bootstrapServers,
            String kafkaUsername,
            String kafkaPassword,
            String activityRecordTopic,
            String maskinportenAuthenticationRecordTopic,
            String maskinportenTokenRecordTopic,
            Integer threadPoolSize,
            Integer threadPoolQueueSize) {
        this.kafkaPassword = kafkaPassword;
        this.kafkaUsername = kafkaUsername;

        Properties eventLoggerDefaultProperties = loadPropertiesFromFile(EVENT_LOGGER_PROPERTIES_FILE_PATH);
        this.featureEnabled = Optional.ofNullable(featureEnabled).orElse(
                Boolean.valueOf(eventLoggerDefaultProperties.getProperty(FEATURE_ENABLED_KEY, "true"))
        );
        this.threadPoolSize = Optional.ofNullable(threadPoolSize).orElse(
                Integer.valueOf(eventLoggerDefaultProperties.getProperty(THREAD_POOL_SIZE_KEY)));
        this.threadPoolQueueSize = Optional.ofNullable(threadPoolQueueSize).orElse(
                Integer.valueOf(eventLoggerDefaultProperties.getProperty(THREAD_POOL_QUEUE_SIZE_KEY)));
        if (this.featureEnabled) {
            this.applicationName = resolveProperty(APPLICATION_NAME, applicationName, eventLoggerDefaultProperties);
            this.environmentName = resolveProperty(ENVIRONMENT_NAME, environmentName, eventLoggerDefaultProperties);
            this.activityRecordTopic = resolveProperty(ACTIVITY_RECORD_TOPIC_KEY, activityRecordTopic, eventLoggerDefaultProperties);
            this.maskinportenAuthenticationRecordTopic = resolveProperty(MP_AUTH_RECORD_TOPIC_KEY, maskinportenAuthenticationRecordTopic, eventLoggerDefaultProperties);
            this.maskinportenTokenRecordTopic = resolveProperty(MP_TOKEN_RECORD_TOPIC_KEY, maskinportenTokenRecordTopic, eventLoggerDefaultProperties);
            this.bootstrapServers = Objects.requireNonNull(bootstrapServers, String.format(
                    NULL_TEMPLATE,
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
            this.producerConfig = Collections.unmodifiableMap(createProducerConfig());
        } else {
            this.bootstrapServers = null;
            this.producerConfig = null;
            this.applicationName = "N/A";
            this.environmentName = "N/A";
            this.activityRecordTopic = "N/A";
            this.maskinportenAuthenticationRecordTopic = "N/A";
            this.maskinportenTokenRecordTopic = "N/A";
        }
    }

    private static Map<String, ?> propertiesToMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())));
    }

    private static String resolveProperty(String key, String userSpecifiedValue, Properties defaultProperties) {
        if (Strings.isNullOrEmpty(userSpecifiedValue)) {
            return Objects.requireNonNull(defaultProperties.getProperty(key), String.format("No default %s found", key));
        } else {
            return userSpecifiedValue;
        }
    }


    /**
     * <a href="https://github.com/felleslosninger/eid-log-event/issues/174">...</a>
     *
     * @param bootstrapServers URL to Kafka
     * @param configured       value
     * @return SASL mechanism appropriate for external or internal URL
     */
    static String resolveSaslMechanism(String bootstrapServers, String configured) {
        if (bootstrapServers.contains("local")) {
            if (!SecurityProtocol.SASL_PLAINTEXT.name().equals(configured)) {
                log.info(
                        "{} overridden with value {}",
                        SaslConfigs.SASL_MECHANISM, SecurityProtocol.SASL_PLAINTEXT.name());
            }
            return SecurityProtocol.SASL_PLAINTEXT.name();
        } else {
            return configured;
        }
    }

    private Map<String, Object> createProducerConfig() {
        Properties kafkaProducerProperties = loadPropertiesFromFile(PRODUCER_PROPERTIES_FILE_PATH);
        kafkaProducerProperties =
                overrideWithOptionalConfig(kafkaProducerProperties, CUSTOM_PRODUCER_PROPERTIES_FILE_PATH);
        Map<String, Object> producerConfig = new HashMap<>(propertiesToMap(kafkaProducerProperties));
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(SaslConfigs.SASL_MECHANISM, resolveSaslMechanism(
                bootstrapServers,
                kafkaProducerProperties.getProperty(SaslConfigs.SASL_MECHANISM)));
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

}
