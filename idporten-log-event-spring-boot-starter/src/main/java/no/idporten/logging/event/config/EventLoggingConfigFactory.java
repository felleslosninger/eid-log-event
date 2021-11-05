package no.idporten.logging.event.config;

import no.idporten.logging.event.EventLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class EventLoggingConfigFactory {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "digdir.event.logging")
    public EventLoggingConfig eventLoggingConfig() {
        return new EventLoggingConfig();
    }

    @Bean(name = "AsyncPublisherPool")
    public ExecutorService publisherPool() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventLogger eventLogger(
            EventLoggingConfig eventLoggingConfig,
            @Qualifier("AsyncPublisherPool") ExecutorService executorService) {
        return new EventLogger(eventLoggingConfig, executorService);
    }
}
