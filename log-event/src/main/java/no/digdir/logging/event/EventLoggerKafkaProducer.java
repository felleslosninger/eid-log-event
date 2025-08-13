package no.digdir.logging.event;

import org.apache.kafka.clients.producer.KafkaProducer;

class EventLoggerKafkaProducer extends KafkaProducer<String, String> {

    EventLoggerKafkaProducer(EventLoggingConfig config) {
        super(config.getProducerConfig());
    }
}
