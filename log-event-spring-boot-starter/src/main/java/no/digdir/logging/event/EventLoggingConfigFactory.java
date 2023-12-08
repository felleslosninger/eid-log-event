package no.digdir.logging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;

import static no.digdir.logging.event.EventLoggingConfig.FEATURE_ENABLED_KEY;

@Slf4j
@AutoConfiguration
class EventLoggingConfigFactory {

    @AutoConfiguration
    @ConditionalOnProperty(prefix = "digdir.event.logging", name = "feature-enabled", havingValue = "true", matchIfMissing = true)
    @EnableConfigurationProperties(EventLoggingConfigurationProperties.class)
    public static class Enabled {

        @Value("${spring.application.name}")
        private String applicationName;

        @Bean
        @ConditionalOnMissingBean(EventLoggingConfig.class)
        EventLoggingConfig eventLoggingConfig(EventLoggingConfigurationProperties eventLoggingConfigurationProperties) {
            return EventLoggingConfig.builder()
                    .applicationName(applicationName)
                    .environmentName(eventLoggingConfigurationProperties.getEnvironmentName())
                    .bootstrapServers(eventLoggingConfigurationProperties.getBootstrapServers())
                    .featureEnabled(eventLoggingConfigurationProperties.isFeatureEnabled())
                    .activityRecordTopic(eventLoggingConfigurationProperties.getActivityRecordTopic())
                    .maskinportenTokenRecordTopic(eventLoggingConfigurationProperties.getMaskinportenTokenRecordTopic())
                    .maskinportenAuthenticationRecordTopic(eventLoggingConfigurationProperties.getMaskinportenAuthenticationRecordTopic())
                    .kafkaUsername(eventLoggingConfigurationProperties.getKafkaUsername())
                    .kafkaPassword(eventLoggingConfigurationProperties.getKafkaPassword())
                    .schemaRegistryUrl(eventLoggingConfigurationProperties.getSchemaRegistryUrl())
                    .schemaRegistryPassword(eventLoggingConfigurationProperties.getSchemaRegistryPassword())
                    .schemaRegistryUsername(eventLoggingConfigurationProperties.getSchemaRegistryUsername())
                    .threadPoolSize(eventLoggingConfigurationProperties.getThreadPoolSize())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean(EventLogger.class)
        public EventLogger defaultEventLogger(
                EventLoggingConfig eventLoggingConfig,
                @Qualifier("eventLoggerProducer") Producer<String, SpecificRecordBase> eventLoggerProducer,
                @Qualifier("eventLoggerExecutorService") ExecutorService eventLoggerExecutorService) {
            return new DefaultEventLogger(eventLoggingConfig, eventLoggerProducer, eventLoggerExecutorService);
        }

        @Bean(name = "eventLoggerProducer")
        @ConditionalOnMissingBean(name = "eventLoggerProducer")
        public Producer<String, SpecificRecordBase> eventLoggerProducer(EventLoggingConfig config) {
            return new EventLoggerKafkaProducer(config);
        }

        @Bean(name = "eventLoggerExecutorService")
        @ConditionalOnMissingBean(name = "eventLoggerExecutorService")
        public ExecutorService eventLoggerThreadPoolExecutor(EventLoggingConfig config) {
            return new EventLoggerThreadPoolExecutor(config);
        }
    }

    @AutoConfiguration
    @ConditionalOnProperty(prefix = "digdir.event.logging", name = "feature-enabled", havingValue = "false")
    @EnableConfigurationProperties(EventLoggingConfigurationProperties.class)
    public static class Disabled {

        @Bean
        @ConditionalOnMissingBean(EventLogger.class)
        public EventLogger noopEventLogger() {
            log.info("Event logging disabled through property {}={}", FEATURE_ENABLED_KEY, false);
            return new NoLoggingEventLogger();
        }
    }
}