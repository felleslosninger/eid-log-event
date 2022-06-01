package no.digdir.logging.event;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static no.digdir.logging.event.EventLoggingConfig.ACTIVITY_RECORD_TOPIC_KEY;
import static no.digdir.logging.event.EventLoggingConfig.BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO;
import static no.digdir.logging.event.EventLoggingConfig.ENVIRONMENT_NAME;
import static no.digdir.logging.event.EventLoggingConfig.MP_AUTH_RECORD_TOPIC_KEY;
import static no.digdir.logging.event.EventLoggingConfig.MP_TOKEN_RECORD_TOPIC_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class EventLoggingConfigTest {

    @Test
    void environmentNameIsSet() {
        String environmentName = "unit";
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName(environmentName)
                .bootstrapServers("server")
                .kafkaUsername("franz")
                .schemaRegistryUrl("registry")
                .activityRecordTopic("testTopic")
                .maskinportenAuthenticationRecordTopic("authTopic")
                .maskinportenTokenRecordTopic("tokenTopic")
                .build();

        assertEquals(
                environmentName, eventLoggingConfig.getEnvironmentName(),
                "The name in the config does not have the expected value");
        assertFalse(
                eventLoggingConfig.getProducerConfig().containsKey(ENVIRONMENT_NAME),
                "The environment name should not be present in the producerConfig");
    }

    @Test
    void activityRecordTopicIsSet() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .activityRecordTopic("activityRecordTopic")
                .maskinportenAuthenticationRecordTopic("maskinportenAuthenticationRecordTopic")
                .maskinportenTokenRecordTopic("maskinportenTokenRecordTopic")
                .build();

        assertEquals(
                "activityRecordTopic", eventLoggingConfig.getActivityRecordTopic(),
                "The activityRecordTopic in the config has not the expected value");
        assertFalse(eventLoggingConfig.getProducerConfig()
                .containsKey(ACTIVITY_RECORD_TOPIC_KEY), "The activityRecordTopic should not be present in the producerConfig");
    }

    @Test
    void mpAuthRecordTopicIsSet() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .activityRecordTopic("activityRecordTopic")
                .maskinportenAuthenticationRecordTopic("maskinportenAuthenticationRecordTopic")
                .maskinportenTokenRecordTopic("maskinportenTokenRecordTopic")
                .build();

        assertEquals(
                "maskinportenAuthenticationRecordTopic", eventLoggingConfig.getMaskinportenAuthenticationRecordTopic(),
                "The maskinportenAuthenticationRecordTopic in the config has not the expected value");
        assertFalse(eventLoggingConfig.getProducerConfig()
                .containsKey(MP_AUTH_RECORD_TOPIC_KEY), "The maskinportenAuthenticationRecordTopic should not be present in the producerConfig");
    }

    @Test
    void mpTokenRecordTopicIsSet() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .activityRecordTopic("activityRecordTopic")
                .maskinportenAuthenticationRecordTopic("maskinportenAuthenticationRecordTopic")
                .maskinportenTokenRecordTopic("maskinportenTokenRecordTopic")
                .build();

        assertEquals(
                "maskinportenTokenRecordTopic", eventLoggingConfig.getMaskinportenTokenRecordTopic(),
                "The maskinportenTokenRecordTopic in the config has not the expected value");
        assertFalse(eventLoggingConfig.getProducerConfig()
                .containsKey(MP_TOKEN_RECORD_TOPIC_KEY), "The maskinportenTokenRecordTopic should not be present in the producerConfig");
    }

    @Test
    void eventTopicDefault() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .build();

        assertEquals("aktiviteter", eventLoggingConfig.getActivityRecordTopic(), "The eventTopic default value aktiviteter should be there when its not provided in the builder");
        assertFalse(eventLoggingConfig.getProducerConfig()
                .containsKey(ACTIVITY_RECORD_TOPIC_KEY), "The eventTopic should not be present in the producerConfig");
    }

    @Test
    void applicationNameIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .environmentName("unit")
                .kafkaUsername("franz")
                .bootstrapServers("server")
                .schemaRegistryUrl("registry")
                .build(), "ApplicationName is a required field");
    }

    @Test
    void environmentNameIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .kafkaUsername("franz")
                .bootstrapServers("server")
                .schemaRegistryUrl("registry")
                .build(), "EnvironmentName is a required field");
    }

    @Test
    void bootstrapServersIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .build(), "BootStrapServers is a required field");
    }

    @Test
    void schemaRegistryUrlIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .kafkaUsername("abc")
                .bootstrapServers("abc")
                .build(), "schemaRegistryUrl is a required field");
    }

    @Test
    void kafkaUsernameIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .schemaRegistryUrl("abc")
                .build(), "kafkaUsername is a required field");
    }

    @Test
    void testFeatureEnabledDefault() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .schemaRegistryUrl("abc")
                .kafkaUsername("abc")
                .build();
        assertTrue(config.isFeatureEnabled(), "Feature should be enabled by default");
    }

    @Test
    void testFeatureEnabled() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .schemaRegistryUrl("abc")
                .kafkaUsername("abc")
                .featureEnabled(true)
                .build();
        assertTrue(config.isFeatureEnabled(), "Feature should be enabled if specifically set");
    }

    @Test
    void testFeatureDisabled() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .schemaRegistryUrl("abc")
                .kafkaUsername("abc")
                .featureEnabled(false)
                .build();
        assertFalse(config.isFeatureEnabled(), "Feature should be disabled if specifically set");
    }

    @Test
    void threadPoolSize() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .schemaRegistryUrl("abc")
                .kafkaUsername("abc")
                .featureEnabled(true)
                .threadPoolSize(20)
                .build();
        assertEquals(20, config.getThreadPoolSize(), "ThreadPoolSize should be equal to builder input");
    }

    @Test
    void threadPoolSizeDefault() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .schemaRegistryUrl("abc")
                .kafkaUsername("abc")
                .featureEnabled(true)
                .build();
        assertEquals(4, config.getThreadPoolSize(), "ThreadPoolSize default should be 4");
    }

    @Test
    void noSchemaRegistryUsername() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .build();

        assertEquals(
                AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE_DEFAULT,
                eventLoggingConfig.getProducerConfig().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "If no schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to 'URL'");
    }

    @Test
    void withSchemaRegistryUsernameAndPassword() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .schemaRegistryUsername("username")
                .schemaRegistryPassword("password")
                .build();

        assertEquals(
                BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO,
                eventLoggingConfig.getProducerConfig().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "When schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to 'USER_INFO'");
        assertEquals("username:password", eventLoggingConfig.getProducerConfig()
                        .get(KafkaAvroSerializerConfig.USER_INFO_CONFIG),
                "The userinfo is expected in the format username:password");
    }

    @Test
    void withSchemaRegistryUsernameAndNoPassword() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .schemaRegistryUsername("username")
                .build();

        assertEquals(
                BASIC_AUTH_CREDENTIALS_SOURCE_USER_INFO,
                eventLoggingConfig.getProducerConfig().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "When schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to 'USER_INFO'");
        assertEquals("username:", eventLoggingConfig.getProducerConfig()
                        .get(KafkaAvroSerializerConfig.USER_INFO_CONFIG),
                "The userinfo is expected in the format username:password, when no password is provided the password should be empty");
    }

    @Test
    void overrideWithOptionalConfig() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("broker")
                .kafkaUsername("user")
                .schemaRegistryUrl("registry")
                .build();

        assertEquals("32601", eventLoggingConfig.getProducerConfig().get("batch.size"));
    }
}