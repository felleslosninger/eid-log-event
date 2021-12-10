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

import static no.idporten.logging.event.EventLoggingConfig.BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO;
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
    void applicationNameSetFromYaml() {
        assertEquals("testEventLogger", String.valueOf(eventLoggingConfig.getApplicationName()));
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
    void schemaRegistryUrlCanBeSetFromYaml() {
        assertEquals("localhost:443", eventLoggingConfig.getProducerConfig()
                .get(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG));
    }

    @Test
    void schemaRegistryUsernameAndPasswordCanBeSetFromYaml() {
        assertEquals(
                BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO,
                eventLoggingConfig.getProducerConfig().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "When schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to 'USER_INFO'");
        assertEquals("schemaUsername:schemaPassword", eventLoggingConfig.getProducerConfig()
                        .get(KafkaAvroSerializerConfig.USER_INFO_CONFIG),
                "The userinfo is expected in the format username:password");
    }

    @Test
    void bootstrapServersCanBeSetFromYaml() {
        assertEquals("localhost:443", eventLoggingConfig.getProducerConfig()
                .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void eventTopicCanBeSetFromYaml() {
        assertEquals("springEventTopic", eventLoggingConfig.getEventTopic(), "application.yml in test/resources are overriding the default eventTopic with springEventTopic");
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
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid("25079494081")
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        eventLogger.log(record);
    }
}