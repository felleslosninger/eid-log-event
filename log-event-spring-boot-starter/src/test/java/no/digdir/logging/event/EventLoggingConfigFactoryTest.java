package no.digdir.logging.event;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Autowired
    @Qualifier("eventLoggerProducer")
    Producer<String, String> eventLoggerProducer;

    @Test
    void applicationNameSetFromYaml() {
        assertEquals("testEventLogger", String.valueOf(eventLoggingConfig.getApplicationName()));
    }

    @Test
    void environmentNameSetFromYaml() {
        assertEquals("dev", String.valueOf(eventLoggingConfig.getEnvironmentName()));
    }

    @Test
    void kafkaUsernameCanBeSetFromYaml() {
        assertTrue(String.valueOf(eventLoggingConfig.getProducerConfig().get(SaslConfigs.SASL_JAAS_CONFIG))
                .contains("username=\"franz\""), "Franz username should be contained in authentication string");
    }

    @Test
    void kafkaPasswordCanBeSetFromYaml() {
        assertTrue(String.valueOf(eventLoggingConfig.getProducerConfig().get(SaslConfigs.SASL_JAAS_CONFIG))
                .contains("password=\"franz123\""), "franz123 password should be contained in authentication string");
    }

    @Test
    void bootstrapServersCanBeSetFromYaml() {
        assertEquals("localhost:443", eventLoggingConfig.getProducerConfig()
                .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void eventTopicCanBeSetFromYaml() {
        assertEquals("springEventTopic", eventLoggingConfig.getActivityRecordTopic(), "application.yml in test/resources are overriding the default eventTopic with springEventTopic");
    }

    @Test
    void threadPoolSizeCanBeSetFromYaml() {
        assertEquals(10, eventLoggingConfig.getThreadPoolSize(), "application.yml in test/resources are overriding the default eventTopic with springEventTopic");
    }


    @Test
    void featureEnabledByDefault() {
        assertTrue(eventLoggingConfig.isFeatureEnabled(), "application.yml in main/resources are defining feature enabled to true as the default starting point");
    }

    @Test
    void eventLoggerLogs() {
        ActivityRecord record = ActivityRecord.builder()
                .eventName("Innlogget")
                .eventSubjectPid("25079494081")
                .correlationId(UUID.randomUUID().toString())
                .serviceProviderId("Ansattportalen")
                .authEid("MinID")
                .authMethod("OTC")
                .build();

        eventLogger.log(record);

        assertThat(eventLogger).isInstanceOf(DefaultEventLogger.class);
        assertThat(eventLoggerProducer).isInstanceOf(EventLoggerKafkaProducer.class);
    }
}