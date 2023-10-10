package no.digdir.logging.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Based upon DiscardOldestPolicy, modified for our needs.
 *
 * @see java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy
 */
class DiscardAndLogOldestPolicy implements RejectedExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(DiscardAndLogOldestPolicy.class);

    /**
     * Obtains, logs and ignores the next task that the executor
     * would otherwise execute, if one is immediately available,
     * and then retries execution of task r, unless the executor
     * is shut down, in which case task r is instead discarded.
     *
     * @param r the runnable task requested to be executed
     * @param e the executor attempting to execute this task
     */
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (!e.isShutdown()) {
            Runnable runnableToBeDiscarded = e.getQueue().poll();
            if (runnableToBeDiscarded instanceof KafkaTask) {
                log.warn("Queue is full, discarding event: {}", ((KafkaTask) runnableToBeDiscarded).getProducerRecord()
                        .value());
            } else {
                if (runnableToBeDiscarded != null) {
                    log.warn("Discarded runnable of unknown type. It was: " + runnableToBeDiscarded.getClass());
                }
            }
            e.execute(r);
        }
    }
}
