package no.digdir.logging.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

@SpringBootTest(classes = ApplicationTest.class)
@ContextConfiguration(
        classes = EventLoggingConfigFactory.class,
        initializers = ConfigDataApplicationContextInitializer.class)
public class MaskinportenLoggerTest {
    @Autowired
    private EventLoggingConfig eventLoggingConfig;

    @Test
    void logMaskinportenEvent() {
        EventLogger eventLogger = new EventLogger(eventLoggingConfig);
        MPAuthenticationRecord record = MPAuthenticationRecord.builder()
                .eventName("Token utstedet")
                .correlationId(UUID.randomUUID().toString())
                .certificateIssuer("https://maskinporten.no/")
                .clientId("test client ID")
                .certificateIssuer("0192:999888777")
                .certificateSerialNumber("0192:777888999")
                .eventDescription("lanekassen:lan/v1/saldoopplysninger")
                .build();

        eventLogger.log(record);
    }

}
