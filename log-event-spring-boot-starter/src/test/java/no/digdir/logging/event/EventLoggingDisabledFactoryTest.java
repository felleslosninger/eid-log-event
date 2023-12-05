package no.digdir.logging.event;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ApplicationTest.class,
        properties = {
                "digdir.event.logging.feature-enabled=false",
                "digdir.event.logging.event-topic=activities"
        }
)
@ContextConfiguration(
        classes = EventLoggingConfigFactory.class,
        initializers = ConfigDataApplicationContextInitializer.class)
class EventLoggingDisabledFactoryTest {
    @Autowired
    EventLogger eventLogger;

    @Autowired
    @Qualifier("eventLoggerProducer")
    Producer<String, SpecificRecordBase> eventLoggerProducer;

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

        assertThat(eventLoggerProducer).isInstanceOf(NoLoggingProducer.class);
    }

}
