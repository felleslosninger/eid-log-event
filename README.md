# Event logging

Library for publishing events to a pre-defined Kafka topic.

## idporten-log-event
Core library with minimal dependencies

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
                .bootstrapServers(BROKER_HOST_AND_PORT)
                .schemaRegistryUrl(REGISTRY_HOST_AND_PORT)
                .kafkaUsername(USERNAME)
                .kafkaPassword(PASSWORD)
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

## idporten-log-event-spring-boot-starter
Spring Boot Starter for autoconfiguration of the library

### Build
Import the library with Maven:
```xml
    <dependency>
        <groupId>no.idporten.logging</groupId>
        <artifactId>idporten-log-event-spring-boot-starter</artifactId>
        <version>${idporten-event-log.version}</version>
    </dependency>
```
### Configuration
The library is configured through the `application.yml` file.
```yaml
digdir:
  event:
    logging:
      bootstrap-servers: example.com:80
      schema-registry-url: example.com:80
      kafka-username: kafka
```
### Usage
Simply wire in the Spring Boot-configured `EventLogger`:
```java
    @Autowired
    EventLogger eventLogger;
```
[...]
```java
            EventRecord record = EventRecord.newBuilder()
            .setName("Innlogget")
            .setPid("25079494081")
            .setCorrelationId(UUID.randomUUID().toString())
            .setService("idPorten")
            .build();

            eventLogger.log(record);
```