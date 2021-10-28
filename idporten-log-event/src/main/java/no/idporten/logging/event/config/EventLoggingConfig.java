package no.idporten.logging.event.config;

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Data
@Builder
@Slf4j
public class EventLoggingConfig {
    private static final String PROPERTIES_FILE_PATH = "kafka.properties";
    @NonNull
    private String keySerializer;
    @NonNull
    private String valueSerializer;
    @NonNull
    private String brokerUrl;
    @NonNull
    private String schemaRegistryUrl;
    @NonNull
    private String eventTopic;

    private String bootstrapServers;
    private String securityProtocol;
    private String saslMechanism;
    private String saslJaasConfig;

    private Properties properties;

    private static Map<String, ?> convertToMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())));
    }

    static BiFunction<String, Object, Object> replaceIfSet(final String value) {
        return (k, v) -> value == null ? v : value;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> configMap = new HashMap<>();

        // basic properties
        properties = loadProperties();
        if (properties != null && !properties.isEmpty()) {
            configMap.putAll(convertToMap(properties));
        }

        // required configuration
        configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        configMap.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        // security
        configMap.compute(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, replaceIfSet(bootstrapServers));
        configMap.compute(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, replaceIfSet(securityProtocol));
        configMap.compute(SaslConfigs.SASL_MECHANISM, replaceIfSet(saslMechanism));
        configMap.compute(SaslConfigs.SASL_JAAS_CONFIG, replaceIfSet(saslJaasConfig));

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
