package no.digdir.logging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
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
            .kafkaUsername("user")
            .threadPoolSize(1)
            .build();
    private final Producer<String, String> kafkaProducer = new KafkaProducer<>(config.getProducerConfig());
    private final DefaultEventLogger eventLogger = new DefaultEventLogger(config, kafkaProducer, executor);

    @Test
    void testOldestIsDescheduled() throws JsonProcessingException {
        KafkaTask oldKafkaTask = createKafkaTask("OldEvent");
        when(executor.getQueue()).thenReturn(queue);
        when(queue.poll()).thenReturn(oldKafkaTask);

        KafkaTask newKafkaTask = createKafkaTask("NewEvent");

        discardAndLogOldestPolicy.rejectedExecution(newKafkaTask, executor);
        verify(executor).execute(newKafkaTask);
        verify(queue).poll();
    }

    private KafkaTask createKafkaTask(String eventName) throws JsonProcessingException {
        return new KafkaTask(eventLogger.toProducerRecord(ActivityRecord.builder()
                .eventName(eventName)
                .eventDescription("Brukeren har logget inn")
                .eventSubjectPid("123")
                .correlationId(UUID.randomUUID().toString())
                .serviceProviderId("McDuck IT")
                .serviceOwnerId("Andeby kommune")
                .authEid("MinID")
                .authMethod("OTC")
                .build()), kafkaProducer);
    }
}