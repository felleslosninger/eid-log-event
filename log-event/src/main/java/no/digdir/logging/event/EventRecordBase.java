package no.digdir.logging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
public abstract class EventRecordBase {
    @JsonProperty("event_name")
    private final String eventName;
    @JsonProperty("event_created")
    private final Instant eventCreated;
    @JsonProperty("event_description")
    private final String eventDescription;
    @JsonProperty("correlation_id")
    private final String correlationId;
    @JsonProperty("extra_data")
    private final Map<String, String> extraData;
    @JsonProperty("application_name")
    @Setter
    private String applicationName;
    @JsonProperty("application_environment")
    @Setter
    private String applicationEnvironment;

    protected EventRecordBase(
            String eventName,
            String eventDescription,
            String correlationId,
            Map<String, String> extraData,
            Instant eventCreated) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(eventName), "eventName is a required field");
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.correlationId = correlationId;
        this.extraData = extraData;
        if (eventCreated != null) {
            this.eventCreated = eventCreated;
        } else {
            this.eventCreated = Instant.now();
        }
    }
}
