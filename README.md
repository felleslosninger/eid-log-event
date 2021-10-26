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
import no.idporten.logging.event.config.EventLoggingConfig;
[...]
        EventLoggingConfig config = EventLoggingConfig.builder()
                .keySerializer(StringSerializer.class.getName())
                .valueSerializer(KafkaAvroSerializer.class.getName())
                .brokerUrl(DUMMY_URL)
                .schemaRegistryUrl(DUMMY_URL)
                .eventTopic(EVENT_TOPIC)
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