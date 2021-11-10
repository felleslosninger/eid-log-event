package no.idporten.logging.event;

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

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

    @Test
    void propertiesAreSet() {
        assertEquals("localhost:443", eventLoggingConfig.getProducerConfig()
                .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("localhost:443", eventLoggingConfig.getProducerConfig()
                .get(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG));
        assertEquals("springEventTopic", eventLoggingConfig.getEventTopic());
        assertTrue(eventLoggingConfig.isFeatureEnabled());
        assertTrue(String.valueOf(eventLoggingConfig.getProducerConfig().get(SaslConfigs.SASL_JAAS_CONFIG))
                .contains("franz"), "Franz username should be contained in authentication string");
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