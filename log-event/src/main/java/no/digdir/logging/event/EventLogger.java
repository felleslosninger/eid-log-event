package no.digdir.logging.event;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.util.Map;

public interface EventLogger {
    void log(EventRecordBase eventRecord);

    Map<MetricName, ? extends Metric> getMetrics();
}
