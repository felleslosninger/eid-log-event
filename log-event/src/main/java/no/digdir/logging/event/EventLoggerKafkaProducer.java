package no.digdir.logging.event;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;

class EventLoggerKafkaProducer extends KafkaProducer<String, SpecificRecordBase> {

    EventLoggerKafkaProducer(EventLoggingConfig config) {
        super(config.getProducerConfig());
    }
}
