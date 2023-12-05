package no.digdir.logging.event;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.util.Collections;
import java.util.Map;

public class NoLoggingEventLogger implements EventLogger {

    @Override
    public void log(EventRecordBase eventRecord) {
        // NOOP
    }

    @Override
    public Map<MetricName, ? extends Metric> getMetrics() {
        return Collections.emptyMap();
    }
}
