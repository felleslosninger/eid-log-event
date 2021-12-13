package no.idporten.logging.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(EventLogger.class)
@EnableConfigurationProperties(EventLoggingConfigurationProperties.class)
class EventLoggingConfigFactory {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    public EventLoggingConfig eventLoggingConfig(EventLoggingConfigurationProperties eventLoggingConfigurationProperties) {
        return EventLoggingConfig.builder()
                .applicationName(applicationName)
                .environmentName(eventLoggingConfigurationProperties.getEnvironmentName())
                .bootstrapServers(eventLoggingConfigurationProperties.getBootstrapServers())
                .featureEnabled(eventLoggingConfigurationProperties.isFeatureEnabled())
                .eventTopic(eventLoggingConfigurationProperties.getEventTopic())
                .kafkaUsername(eventLoggingConfigurationProperties.getKafkaUsername())
                .kafkaPassword(eventLoggingConfigurationProperties.getKafkaPassword())
                .schemaRegistryUrl(eventLoggingConfigurationProperties.getSchemaRegistryUrl())
                .schemaRegistryPassword(eventLoggingConfigurationProperties.getSchemaRegistryPassword())
                .schemaRegistryUsername(eventLoggingConfigurationProperties.getSchemaRegistryUsername())
                .threadPoolSize(eventLoggingConfigurationProperties.getThreadPoolSize())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventLogger eventLogger(EventLoggingConfig eventLoggingConfig) {
        return new EventLogger(eventLoggingConfig);
    }
}
