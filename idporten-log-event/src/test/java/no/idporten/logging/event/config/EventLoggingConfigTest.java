package no.idporten.logging.event.config;

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventLoggingConfigTest {

    @Test
    void noSchemaRegistryUsername() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .build();

        assertEquals("URL", eventLoggingConfig.toMap().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "If no schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to URL");
    }

    @Test
    void withSchemaRegistryUsernameAndPassword() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .schemaRegistryUsername("username")
                .schemaRegistryPassword("password")
                .build();

        assertEquals("USER_INFO", eventLoggingConfig.toMap().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "When schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to USER_INFO");
        assertEquals("username:password", eventLoggingConfig.toMap().get(KafkaAvroSerializerConfig.USER_INFO_CONFIG),
                "The userinfo is expected in the format username:password");
    }

    @Test
    void withSchemaRegistryUsernameAndNoPassword() {
        EventLoggingConfig eventLoggingConfig = EventLoggingConfig.builder()
                .bootstrapServers("abc")
                .kafkaUsername("abc")
                .schemaRegistryUrl("abc")
                .schemaRegistryUsername("username")
                .build();

        assertEquals("USER_INFO", eventLoggingConfig.toMap().get(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE),
                "When schemaRegistryUsername is provided, the authentication against schemaRegistry should be set to USER_INFO");
        assertEquals("username:", eventLoggingConfig.toMap().get(KafkaAvroSerializerConfig.USER_INFO_CONFIG),
                "The userinfo is expected in the format username:password, when no password is provided the password should be empty");
    }

}