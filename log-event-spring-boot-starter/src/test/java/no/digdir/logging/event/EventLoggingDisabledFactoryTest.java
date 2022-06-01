package no.digdir.logging.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

@SpringBootTest(
        classes = ApplicationTest.class,
        properties = {
                "digdir.event.logging.feature-enabled=false",
                "digdir.event.logging.event-topic=aktiviteter"
        }
)
@ContextConfiguration(
        classes = EventLoggingConfigFactory.class,
        initializers = ConfigDataApplicationContextInitializer.class)
class EventLoggingDisabledFactoryTest {
    @Autowired
    EventLogger eventLogger;

    @Test
    void eventLoggerDisabled() {
        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid("25079494081")
                .correlationId(UUID.randomUUID().toString())
                .serviceProviderId("NAV")
                .authEid("CommFides")
                .authMethod("PIN")
                .build();

        eventLogger.log(record);
    }

}
