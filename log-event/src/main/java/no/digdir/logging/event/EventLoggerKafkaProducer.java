package no.digdir.logging.event;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;

public class EventLoggerKafkaProducer extends KafkaProducer<String, SpecificRecordBase> {

    public EventLoggerKafkaProducer(EventLoggingConfig config) {
        super(config.getProducerConfig());
    }
}
