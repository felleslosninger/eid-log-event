package no.digdir.logging.event;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static no.digdir.logging.event.EventLoggingConfig.ACTIVITY_RECORD_TOPIC_KEY;
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
                .build();

        assertEquals("aktiviteter-json", eventLoggingConfig.getActivityRecordTopic(), "The eventTopic default value activities should be there when its not provided in the builder");
        assertFalse(eventLoggingConfig.getProducerConfig()
                .containsKey(ACTIVITY_RECORD_TOPIC_KEY), "The eventTopic should not be present in the producerConfig");
    }

    @Test
    void applicationNameIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .environmentName("unit")
                .kafkaUsername("franz")
                .bootstrapServers("server")
                .build(), "ApplicationName is a required field");
    }

    @Test
    void environmentNameIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .kafkaUsername("franz")
                .bootstrapServers("server")
                .build(), "EnvironmentName is a required field");
    }

    @Test
    void bootstrapServersIsRequired() {
        assertThrows(NullPointerException.class, () -> EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .kafkaUsername("abc")
                .build(), "BootStrapServers is a required field");
    }

    @Test
    void testFeatureEnabledDefault() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
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
                .kafkaUsername("abc")
                .featureEnabled(true)
                .threadPoolSize(20)
                .build();
        assertEquals(20, config.getThreadPoolSize(), "ThreadPoolSize should be equal to builder input");
    }

    @Test
    void threadPoolQueueSize() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .featureEnabled(true)
                .threadPoolQueueSize(200)
                .build();
        assertEquals(200, config.getThreadPoolQueueSize(), "ThreadPoolQueueSize should be equal to builder input");
    }

    @Test
    void threadPoolQueueSizeDefault() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .featureEnabled(true)
                .build();
        assertEquals(30000, config.getThreadPoolQueueSize(), "ThreadPoolQueueSize default should be 100000");
    }

    @Test
    void threadPoolSizeDefault() {
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .featureEnabled(true)
                .build();
        assertEquals(4, config.getThreadPoolSize(), "ThreadPoolSize default should be 4");
    }

    @Test
    void overrideWithOptionalConfig() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .applicationName("testApplicationName")
                .environmentName("unit")
                .bootstrapServers("broker")
                .kafkaUsername("user")
                .build();

        assertEquals("32601", eventLoggingConfig.getProducerConfig().get("batch.size"));
    }

    @Test
    void resolveSaslMechanism() {
        String localUrl = "strimzi-kafka-kafka-bootstrap.event-statistikk.svc.cluster.local:9092";
        String externalUrl = "kafka.systest.eid-event-stat.no:443";
        assertEquals("SASL_PLAINTEXT", EventLoggingConfig.resolveSaslMechanism(localUrl, "SASL_SSL"));
        assertEquals("SASL_SSL", EventLoggingConfig.resolveSaslMechanism(externalUrl, "SASL_SSL"));
    }
}