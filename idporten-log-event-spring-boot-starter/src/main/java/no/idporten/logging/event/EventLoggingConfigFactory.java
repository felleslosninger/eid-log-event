package no.idporten.logging.event;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
class EventLoggingConfigFactory {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "digdir.event.logging")
    EventLoggingConfig eventLoggingConfig() {
        return new EventLoggingConfig();
    }

    @Bean(name = "AsyncPublisherPool")
    ExecutorService publisherPool() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    EventLogger eventLogger(
            EventLoggingConfig eventLoggingConfig,
            @Qualifier("AsyncPublisherPool") ExecutorService executorService) {
        return new EventLogger(eventLoggingConfig, executorService);
    }
}
