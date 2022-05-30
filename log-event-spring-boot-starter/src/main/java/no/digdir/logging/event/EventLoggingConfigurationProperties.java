package no.digdir.logging.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.Optional;

@ConfigurationProperties(prefix = "digdir.event.logging")
@ConstructorBinding
@Getter(AccessLevel.PACKAGE)
class EventLoggingConfigurationProperties {
    private final boolean featureEnabled;
    private final String environmentName;
    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final String kafkaUsername;
    private final String kafkaPassword;
    private final String schemaRegistryUsername;
    private final String schemaRegistryPassword;
    private final String eventTopic;
    private final Integer threadPoolSize;

    EventLoggingConfigurationProperties(
            Boolean featureEnabled,
            String environmentName,
            String bootstrapServers,
            String schemaRegistryUrl,
            String kafkaUsername,
            String kafkaPassword,
            String schemaRegistryUsername,
            String schemaRegistryPassword,
            String eventTopic,
            Integer threadPoolSize) {
        this.featureEnabled = Optional.ofNullable(featureEnabled).orElse(true);
        this.environmentName = environmentName;
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.kafkaUsername = kafkaUsername;
        this.kafkaPassword = kafkaPassword;
        this.schemaRegistryUsername = schemaRegistryUsername;
        this.schemaRegistryPassword = schemaRegistryPassword;
        this.eventTopic = eventTopic;
        this.threadPoolSize = threadPoolSize;
    }
}
