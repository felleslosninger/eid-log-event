# Event logging

Library for publishing events to Kafka.

## log-event
Core library with minimal dependencies

### Build

Pick a [release](https://github.com/felleslosninger/eid-log-event/releases) 
and import the library with Maven:

```xml
    <dependency>
        <groupId>no.digdir.logging</groupId>
        <artifactId>log-event</artifactId>
        <version>${log-event.version}</version>
    </dependency>
```
Since we use libraries from [Confluent](https://confluent.io), you may need to add their repository:
```xml
    <repositories>
        <repository>
            <id>Confluent</id>
            <name>Confluent Kafka</name>
            <url>https://packages.confluent.io/maven/</url>
        </repository>
    </repositories>
```
### Configuration

A Kafka-client needs to find servers and uses a thread-pool to publish events.
Use `EventLoggingConfig.builder()` to configure the settings:

```java
import no.digdir.logging.event.EventLogger;
import no.digdir.logging.event.EventLoggingConfig;
[...]
        EventLoggingConfig config = EventLoggingConfig.builder()
                .applicationName(APPLICATION_NAME)
                .environmentName(ENVIRONMENT_NAME)
                .bootstrapServers(BROKER_HOST_AND_PORT)
                .schemaRegistryUrl(REGISTRY_HOST_AND_PORT)
                .kafkaUsername(USERNAME)
                .kafkaPassword(PASSWORD)
                .threadPoolSize(8) // Defaults to 4 if not set
                .build();

        EventLogger eventLogger = new EventLogger(config);
```

### Usage

Use `ActivityRecord.newBuilder()` to create an entry to publish:

```java
import no.digdir.logging.event.ActivityRecord;
[...]
        ActivityRecord record = ActivityRecord.newBuilder()
                .setEventName("Innlogget")
                .setEventSubjectPid("25079494081")
                .setCorrelationId(UUID.randomUUID().toString())
                .setServiceProviderId("idPorten-123")
                .setServiceProviderOrgno("123123123")
                .setServiceOwnerId("idPorten-123")
                .setServiceOwnerOrgno("123123123")
                .build();
        eventLogger.log(record);
```

Explore the `no.digdir.logging.event.ActivityRecord` class for further optional attributes.
The `eventCreated`-attribute will default to current time, if not specified.

## log-event-spring-boot-starter
Spring Boot Starter for autoconfiguration of the library

### Build
Import the library with Maven:
```xml
    <dependency>
        <groupId>no.digdir.logging</groupId>
        <artifactId>log-event-spring-boot-starter</artifactId>
        <version>${log-event.version}</version>
    </dependency>
```
### Configuration
The library is configured through the `application.yml` file.
```yaml
digdir:
  event:
    logging:
      environment-name: dev
      bootstrap-servers: example.com:80
      schema-registry-url: example.com:80
      kafka-username: kafkaUsername
      kafka-password: kafkaPassword
      schema-registry-password: schemaPassword
      schema-registry-username: schemaUsername
      activity-record-topic: activityTopic #Do not set unless you know you want to ship to different location than the default topic
      maskinporten-token-record-topic: mpTokenTopic #Do not set unless you know you want to ship to different location than the default topic
      maskinporten-authentication-record-topic: mpAuthTopic #Do not set unless you know you want to ship to different location than the default topic
      thread-pool-size: 8 # Defaults to 4 if not set

spring:
  application:
    name: myApplication

```
### Usage
Simply wire in the Spring Boot-configured `EventLogger`:
```java
    @Autowired
    EventLogger eventLogger;
```
[...]

Then create a record of the required type and send it to the eventLogger.
Currently we support three different types of records: ActivityRecord, MPAuthenticationRecord and MPTokenIssuedRecord
```java
            ActivityRecord record = ActivityRecord.newBuilder()
            .setEventName("Innlogget")
            .setEventSubjectPid("25079494081")
            .setCorrelationId(UUID.randomUUID().toString())
            .setServiceProviderId("idPorten-123")
            .setServiceProviderOrgno("123123123")
            .setServiceOwnerId("idPorten-123")
            .setServiceOwnerOrgno("123123123")
            .build();
            
            eventLogger.log(record);            
```

Explore the `no.digdir.logging.event.ActivityRecord`, `no.digdir.logging.event.MPAuthenticationRecord` and `no.digdir.logging.event.MPTokenIssuedRecord` classes for further optional attributes.

The `created`-attribute will default to current time, if not specified.

## maskinporten-log-event
For publishing events related to tokens from **Maskinporten**, use `no.digdir.logging.event.MPAuthenticationRecord` and `no.digdir.logging.event.MPTokenIssuedRecord` classes



## Feature toggling
Publishing to Kafka can be disabled by setting the `digdir.event.logging.feature-enabled` property to `false`.

### In log-event
When only the core library is used, the property is set in `event-logger.properties`.

### In log-event-spring-boot-starter
When using the Spring Boot Starter, the property is set in `application.yml`. 

## Tuning
Kafka producer properties may be overriden by providing a file named `custom-kafka-producer.properties` at the root of 
the classpath with entries from the [Kafka documentation](http://kafka.apache.org/documentation.html#producerconfigs). 