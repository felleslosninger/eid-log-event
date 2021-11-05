package no.idporten.logging.event.config;

import no.idporten.logging.event.EventLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventLoggingConfigFactory {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "digdir.event.logging")
    public EventLoggingConfig eventLoggingConfig() {
        return new EventLoggingConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventLogger eventLogger(EventLoggingConfig eventLoggingConfig) {
        return new EventLogger(eventLoggingConfig);
    }
}
