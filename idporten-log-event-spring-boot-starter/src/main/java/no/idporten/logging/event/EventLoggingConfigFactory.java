package no.idporten.logging.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(EventLogger.class)
@EnableConfigurationProperties(EventLoggingConfigurationProperties.class)
class EventLoggingConfigFactory {

    @Bean
    @ConditionalOnMissingBean
    public EventLoggingConfig eventLoggingConfig(EventLoggingConfigurationProperties eventLoggingConfigurationProperties) {
        return EventLoggingConfig.builder()
                .bootstrapServers(eventLoggingConfigurationProperties.getBootstrapServers())
                .featureEnabled(eventLoggingConfigurationProperties.isFeatureEnabled())
                .eventTopic(eventLoggingConfigurationProperties.getEventTopic())
                .kafkaUsername(eventLoggingConfigurationProperties.getKafkaUsername())
                .kafkaPassword(eventLoggingConfigurationProperties.getKafkaPassword())
                .schemaRegistryUrl(eventLoggingConfigurationProperties.getSchemaRegistryUrl())
                .schemaRegistryPassword(eventLoggingConfigurationProperties.getSchemaRegistryPassword())
                .schemaRegistryUsername(eventLoggingConfigurationProperties.getSchemaRegistryUsername())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventLogger eventLogger(EventLoggingConfig eventLoggingConfig) {
        return new EventLogger(eventLoggingConfig);
    }
}
