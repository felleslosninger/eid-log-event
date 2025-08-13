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
    private final String kafkaUsername;
    private final String kafkaPassword;
    private final String activityRecordTopic;
    private final String maskinportenTokenRecordTopic;
    private final String maskinportenAuthenticationRecordTopic;
    private final Integer threadPoolSize;

    EventLoggingConfigurationProperties(
            Boolean featureEnabled,
            String environmentName,
            String bootstrapServers,
            String kafkaUsername,
            String kafkaPassword,
            String activityRecordTopic,
            String maskinportenTokenRecordTopic,
            String maskinportenAuthenticationRecordTopic,
            Integer threadPoolSize) {
        this.featureEnabled = Optional.ofNullable(featureEnabled).orElse(true);
        this.environmentName = environmentName;
        this.bootstrapServers = bootstrapServers;
        this.kafkaUsername = kafkaUsername;
        this.kafkaPassword = kafkaPassword;
        this.activityRecordTopic = activityRecordTopic;
        this.maskinportenTokenRecordTopic = maskinportenTokenRecordTopic;
        this.maskinportenAuthenticationRecordTopic = maskinportenAuthenticationRecordTopic;
        this.threadPoolSize = threadPoolSize;
    }
}
