package no.idporten.logging.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EventLoggingConfigFactory {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "digdir.event.logging")
    EventLoggingConfig eventLoggingConfig() {
        return new EventLoggingConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    EventLogger eventLogger(EventLoggingConfig eventLoggingConfig) {
        return new EventLogger(eventLoggingConfig);
    }
}
