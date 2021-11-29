package no.idporten.logging.event;

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * No-op class used when the logging feature is disabled
 */
class NoLoggingProducer implements org.apache.kafka.clients.producer.Producer<String, EventRecord> {
    @Override
    public void initTransactions() {
    }

    @Override
    public void beginTransaction() throws ProducerFencedException {
    }

    @Override
    @Deprecated
    public void sendOffsetsToTransaction(
            Map<TopicPartition, OffsetAndMetadata> var1, String var2) throws ProducerFencedException {
    }

    @Override
    public void sendOffsetsToTransaction(
            Map<TopicPartition, OffsetAndMetadata> map,
            ConsumerGroupMetadata consumerGroupMetadata) throws ProducerFencedException {
    }

    @Override
    public void commitTransaction() throws ProducerFencedException {
    }

    @Override
    public void abortTransaction() throws ProducerFencedException {
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, EventRecord> producerRecord) {
        return null;
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, EventRecord> producerRecord, Callback callback) {
        return null;
    }

    @Override
    public void flush() {
    }

    @Override
    public List<PartitionInfo> partitionsFor(String var1) {
        return Collections.emptyList();
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return Collections.emptyMap();
    }

    @Override
    public void close() {
    }

    @Override
    public void close(Duration duration) {
    }
}
