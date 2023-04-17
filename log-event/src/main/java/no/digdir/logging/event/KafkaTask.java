package no.digdir.logging.event;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a convenient way of getting the ProducerRecord out of a submitted runnable task to the executorService.
 * Used in combination with the DiscardAndLogOldestPolicy
 *
 * @see DiscardAndLogOldestPolicy
 */
class KafkaTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(KafkaTask.class);
    private final ProducerRecord<String, SpecificRecordBase> producerRecord;
    private final Producer<String, SpecificRecordBase> producer;

    public KafkaTask(
            ProducerRecord<String, SpecificRecordBase> producerRecord,
            Producer<String, SpecificRecordBase> producer) {
        this.producerRecord = producerRecord;
        this.producer = producer;
    }

    @Override
    public void run() {
        try {
            producer.send(producerRecord, (recordMetadata, e) -> {
                if (e != null) {
                    log.warn("Failed to publish event {}", producerRecord.value(), e);
                } else if (log.isTraceEnabled() && recordMetadata != null) {
                    log.trace("Sent record {} with offset {}", producerRecord, recordMetadata.offset());
                }
            });
        } catch (Exception e) {
            log.warn("Failed to publish event {}", producerRecord.value(), e);
        }
    }

    ProducerRecord<String, SpecificRecordBase> getProducerRecord() {
        return producerRecord;
    }
}