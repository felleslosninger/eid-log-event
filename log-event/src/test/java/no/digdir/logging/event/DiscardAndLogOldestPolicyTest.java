package no.digdir.logging.event;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscardAndLogOldestPolicyTest {

    private final ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
    private final BlockingQueue<Runnable> queue = mock(BlockingQueue.class);
    private final DiscardAndLogOldestPolicy discardAndLogOldestPolicy = new DiscardAndLogOldestPolicy();
    private final EventLoggingConfig config = EventLoggingConfig.builder()
            .applicationName("app")
            .environmentName("env")
            .bootstrapServers("localhost:443")
            .schemaRegistryUrl("localhost:433")
            .kafkaUsername("user")
            .threadPoolSize(1)
            .build();
    private final Producer<String, SpecificRecordBase> kafkaProducer = new KafkaProducer<>(config.getProducerConfig());

    @Test
    void testOldestIsDescheduled() {
        KafkaTask oldKafkaTask = createKafkaTask("OldEvent");
        when(executor.getQueue()).thenReturn(queue);
        when(queue.poll()).thenReturn(oldKafkaTask);

        KafkaTask newKafkaTask = createKafkaTask("NewEvent");

        discardAndLogOldestPolicy.rejectedExecution(newKafkaTask, executor);
        verify(executor).execute(newKafkaTask);
        verify(queue).poll();
    }

    private KafkaTask createKafkaTask(String eventName) {
        return new KafkaTask(ActivityRecord.builder()
                .eventName(eventName)
                .eventDescription("Brukeren har logget inn")
                .eventSubjectPid("123")
                .correlationId(UUID.randomUUID().toString())
                .serviceProviderId("McDuck IT")
                .serviceOwnerId("Andeby kommune")
                .authEid("MinID")
                .authMethod("OTC")
                .build().toProducerRecord(config), kafkaProducer);
    }
}