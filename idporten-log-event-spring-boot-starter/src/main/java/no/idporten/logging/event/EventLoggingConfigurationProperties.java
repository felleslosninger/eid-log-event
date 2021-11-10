package no.idporten.logging.event;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "digdir.event.logging")
@ConstructorBinding
class EventLoggingConfigurationProperties {
    private final boolean featureEnabled;
    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final String kafkaUsername;
    private final String kafkaPassword;
    private final String schemaRegistryUsername;
    private final String schemaRegistryPassword;
    private final String eventTopic;

    EventLoggingConfigurationProperties(
            boolean featureEnabled,
            String bootstrapServers,
            String schemaRegistryUrl,
            String kafkaUsername,
            String kafkaPassword,
            String schemaRegistryUsername, String schemaRegistryPassword, String eventTopic) {
        this.featureEnabled = featureEnabled;
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.kafkaUsername = kafkaUsername;
        this.kafkaPassword = kafkaPassword;
        this.schemaRegistryUsername = schemaRegistryUsername;
        this.schemaRegistryPassword = schemaRegistryPassword;
        this.eventTopic = eventTopic;
    }

    boolean isFeatureEnabled() {
        return featureEnabled;
    }

    String getBootstrapServers() {
        return bootstrapServers;
    }

    String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    String getKafkaUsername() {
        return kafkaUsername;
    }

    String getKafkaPassword() {
        return kafkaPassword;
    }

    String getSchemaRegistryUsername() {
        return schemaRegistryUsername;
    }

    String getSchemaRegistryPassword() {
        return schemaRegistryPassword;
    }

    String getEventTopic() {
        return eventTopic;
    }
}
