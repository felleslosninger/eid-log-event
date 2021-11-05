# Event logging

Library for publishing events to a pre-defined Kafka topic.

### Build
Import the library with Maven:
```xml
    <dependency>
        <groupId>no.idporten.logging</groupId>
        <artifactId>idporten-log-event</artifactId>
        <version>${idporten-event-log.version}</version>
    </dependency>
```

### Configuration
Use `EventLoggingConfig.builder()` to configure settings:
```java
import no.idporten.logging.event.config.EventLogger;
import no.idporten.logging.event.EventLoggingConfig;
[...]
        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers(BROKER_HOST_AND_PORT)
                .schemaRegistryUrl(REGISTRY_HOST_AND_PORT)
                .username(USERNAME)
                .password(PASSWORD)
                .build();

        EventLogger eventLogger = new EventLogger(config);
```

### Usage
Use `EventRecord.newBuilder()` to create an entry to publish:
```java
import no.idporten.logging.event.config.EventRecord;
[...]
        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid("25079494081")
                .setCorrelationId(UUID.randomUUID().toString())
                .setService("idPorten")
                .build();

        eventLogger.log(record);
```