package no.digdir.logging.event;

import lombok.Builder;
import lombok.Getter;
import no.digdir.logging.event.generated.ActivityRecordAvro;
import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ActivityRecord extends EventRecordBase {

    private final String eventActorId;
    private final String eventSubjectPid;
    private final String serviceProviderId;
    private final String serviceProviderOrgno;
    private final String serviceOwnerId;
    private final String serviceOwnerOrgno;
    private final String authEid;
    private final String authMethod;

    @Builder
    public ActivityRecord(
            String eventName,
            String eventDescription,
            String correlationId,
            Map<String, String> extraData,
            String eventActorId,
            String eventSubjectPid,
            String serviceProviderId,
            String serviceProviderOrgno,
            String serviceOwnerId,
            String serviceOwnerOrgno,
            String authEid,
            String authMethod,
            Instant eventCreated) {
        super(eventName, eventDescription, correlationId, extraData, eventCreated);
        this.eventActorId = eventActorId;
        this.eventSubjectPid = eventSubjectPid;
        this.serviceProviderId = serviceProviderId;
        this.serviceProviderOrgno = serviceProviderOrgno;
        this.serviceOwnerId = serviceOwnerId;
        this.serviceOwnerOrgno = serviceOwnerOrgno;
        this.authEid = authEid;
        this.authMethod = authMethod;
    }


    @Override
    protected SpecificRecordBase toAvroObject() {
        return ActivityRecordAvro.newBuilder()
                .setEventName(getEventName())
                .setEventDescription(getEventDescription())
                .setEventCreated(getEventCreated())
                .setApplicationEnvironment(getApplicationEnvironment())
                .setApplicationName(getApplicationName())
                .setCorrelationId(getCorrelationId())
                .setExtraData(getExtraData() == null ? null : getExtraData().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .setEventActorId(eventActorId)
                .setEventSubjectPid(eventSubjectPid)
                .setServiceProviderId(serviceProviderId)
                .setServiceProviderOrgno(serviceProviderOrgno)
                .setServiceOwnerId(serviceOwnerId)
                .setServiceOwnerOrgno(serviceOwnerOrgno)
                .setAuthEid(authEid)
                .setAuthMethod(authMethod)
                .build();
    }
}
