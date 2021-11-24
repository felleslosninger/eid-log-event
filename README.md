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

A Kafka-client needs to find servers and uses a thread-pool to publish events.
Use `EventLoggingConfig.builder()` to configure the settings:

```java
import no.idporten.logging.event.config.EventLogger;
import no.idporten.logging.event.EventLoggingConfig;
[...]
        EventLoggingConfig config = EventLoggingConfig.builder()
                .bootstrapServers(BROKER_HOST_AND_PORT)
                .schemaRegistryUrl(REGISTRY_HOST_AND_PORT)
                .kafkaUsername(USERNAME)
                .kafkaPassword(PASSWORD)
                .threadPoolSize(8) // Defaults to 4 if not set
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
      kafka-username: kafkaUsername
      kafka-password: kafkaPassword
      schema-registry-password: schemaPassword
      schema-registry-username: schemaUsername
      event-topic: eventTopic
      thread-pool-size: 8 # Defaults to 4 if not set
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
## Feature toggling
Publishing to Kafka can be disabled by setting the `digdir.event.logging.feature-enabled` property to `false`.

### In idporten-log-event
When only the core library is used, the property is set in `event-logger.properties`.

### In idporten-log-event-spring-boot-starter
When using the Spring Boot Starter, the property is set in `application.yml`. 