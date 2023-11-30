package no.digdir.logging.event;

import lombok.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EventLoggerThreadPoolExecutor extends ThreadPoolExecutor {

    public EventLoggerThreadPoolExecutor(EventLoggingConfig config) {
        super(config.getThreadPoolSize(), config.getThreadPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(config.getThreadPoolQueueSize()),
                buildThreadFactory(),
                new DiscardAndLogOldestPolicy());
    }

    private static ThreadFactory buildThreadFactory() {
        return new ThreadFactory() {
            private int threadNumber = 0;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "eventLogPool-" + threadNumber++);
            }
        };
    }
}
