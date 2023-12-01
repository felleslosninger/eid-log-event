package no.digdir.logging.event;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.util.Collections;
import java.util.Map;

public interface EventLogger {
    default void log(EventRecordBase eventRecord) {
    }

    default Map<MetricName, ? extends Metric> getMetrics() {
        return Collections.emptyMap();
    }
}
