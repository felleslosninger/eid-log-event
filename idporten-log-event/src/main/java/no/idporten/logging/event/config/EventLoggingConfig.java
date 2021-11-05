package no.idporten.logging.event.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class EventLoggingConfig {
    static final String BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO = "USER_INFO";
    private static final String PROPERTIES_FILE_PATH = "kafka.properties";
    private static final String EVENT_TOPIC_KEY = "event.topic";
    private static final String JAAS_CONFIG_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule " +
            "required username=\"%s\" password=\"%s\";";
    /**
     * Host and port of the kafka broker(s) <BR>
     * (comma-separated list in the case of multiple servers)
     */
    @NonNull
    private String bootstrapServers;

    /**
     * Host and port of the Schema Registry (Confluent)
     */
    @NonNull
    private String schemaRegistryUrl;

    /**
     * Login for the JAAS SASL configuration
     */
    @NonNull
    private String kafkaUsername;

    /**
     * Password for the JAAS SASL configuration
     */
    private String kafkaPassword;

    /**
     * Username for the Schema Registry, leave empty for no authentication
     */
    private String schemaRegistryUsername;

    /**
     * Password for the Schema Registry
     */
    private String schemaRegistryPassword;

    /**
     * Kafka topic to publish to
     */
    private String eventTopic;

    private Properties properties;

    private static Map<String, ?> convertToMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> configMap = new HashMap<>();

        properties = loadProperties();
        if (properties != null && !properties.isEmpty()) {
            configMap.putAll(convertToMap(properties));
        }

        // required configuration
        configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configMap.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        if (!StringUtils.isEmpty(schemaRegistryUsername)) {
            configMap.put(
                    KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
                    BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO);
            configMap.put(
                    KafkaAvroSerializerConfig.USER_INFO_CONFIG,
                    String.format("%s:%s", schemaRegistryUsername, schemaRegistryPassword != null ? schemaRegistryPassword : "")
            );
        } else {
            configMap.put(
                    KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
                    AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE_DEFAULT);
        }
        configMap.put(
                SaslConfigs.SASL_JAAS_CONFIG,
                String.format(JAAS_CONFIG_TEMPLATE, kafkaUsername, kafkaPassword != null ? kafkaPassword : ""));

        if (!StringUtils.isEmpty(eventTopic)) {
            configMap.put(EVENT_TOPIC_KEY, eventTopic);
        } else {
            eventTopic = (String) configMap.get(EVENT_TOPIC_KEY);
        }

        return configMap;
    }

    private Properties loadProperties() {
        if (properties == null) {
            properties = new Properties();
        }

        if (properties.isEmpty()) {
            try {
                InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_PATH);
                properties.load(propertiesStream);
            } catch (Exception e) {
                log.warn("Failed to load properties from {}", PROPERTIES_FILE_PATH, e);
            }
        }
        return properties;
    }
}
