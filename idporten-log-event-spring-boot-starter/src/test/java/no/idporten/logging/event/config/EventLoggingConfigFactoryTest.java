package no.idporten.logging.event.config;

import no.idporten.logging.event.ApplicationTest;
import no.idporten.logging.event.EventLogger;
import no.idporten.logging.event.EventRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ApplicationTest.class)
@ContextConfiguration(
        classes = EventLoggingConfigFactory.class,
        initializers = ConfigDataApplicationContextInitializer.class)
class EventLoggingConfigFactoryTest {
    @Autowired
    EventLogger eventLogger;
    @Autowired
    private EventLoggingConfig eventLoggingConfig;

    @Test
    void propertyIsSet() {
        assertTrue(eventLoggingConfig.getBootstrapServers() != null);
    }

    @Test
    void eventLoggerLogs() {
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid("25079494081")
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        eventLogger.log(record);
    }
}