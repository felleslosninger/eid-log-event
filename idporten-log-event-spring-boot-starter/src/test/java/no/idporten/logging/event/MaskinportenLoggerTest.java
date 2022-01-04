package no.idporten.logging.event;

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
        MaskinportenEventLogger eventLogger = new MaskinportenEventLogger(eventLoggingConfig);
        MaskinportenEventRecord record = MaskinportenEventRecord.newBuilder()
                .setName("Token utstedet")
                .setCorrelationId(UUID.randomUUID().toString())
                .setIss("https://maskinporten.no/")
                .setClientId("test client ID")
                .setClientAmr("virksomhetssertifikat")
                .setConsumer("0192:999888777")
                .setSupplier("0192:777888999")
                .setScope("lanekassen:lan/v1/saldoopplysninger")
                .build();

        eventLogger.log(record);
    }

}
