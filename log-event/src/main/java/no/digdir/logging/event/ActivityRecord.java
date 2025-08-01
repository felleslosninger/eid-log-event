package no.digdir.logging.event;

import lombok.Builder;
import lombok.Getter;
import no.digdir.logging.event.generated.ActivityRecordAvro;
import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;

import static no.digdir.logging.event.DateUtil.computeDOB;

@Getter
public class ActivityRecord extends EventRecordBase {

    private final String eventActorId;
    private final String eventSubjectPid;
    private final String serviceProviderId;
    private final String serviceProviderOrgno;
    private final String serviceProviderName;
    private final String serviceOwnerId;
    private final String serviceOwnerOrgno;
    private final String serviceOwnerName;
    private final String authEid;
    private final String authMethod;
    private final Integer subjectBirthYear;
    private final Integer subjectAgeAtEvent;

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
            String serviceProviderName,
            String serviceOwnerId,
            String serviceOwnerOrgno,
            String serviceOwnerName,
            String authEid,
            String authMethod,
            Integer subjectBirthyear,
            Integer subjectAgeAtEvent,
            Instant eventCreated) {
        super(eventName, eventDescription, correlationId, extraData, eventCreated);
        this.eventActorId = eventActorId;
        this.eventSubjectPid = eventSubjectPid;
        this.serviceProviderId = serviceProviderId;
        this.serviceProviderOrgno = serviceProviderOrgno;
        this.serviceProviderName = serviceProviderName;
        this.serviceOwnerId = serviceOwnerId;
        this.serviceOwnerOrgno = serviceOwnerOrgno;
        this.serviceOwnerName = serviceOwnerName;
        this.authEid = authEid;
        this.authMethod = authMethod;
        this.subjectBirthYear = subjectBirthyear;
        this.subjectAgeAtEvent = subjectAgeAtEvent;
    }


    @Override
    protected SpecificRecordBase toAvroObject() {
        ActivityRecordAvro.Builder recordBuilder = ActivityRecordAvro.newBuilder()
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
                .setServiceProviderName(serviceProviderName)
                .setServiceOwnerId(serviceOwnerId)
                .setServiceOwnerOrgno(serviceOwnerOrgno)
                .setServiceOwnerName(serviceOwnerName)
                .setAuthEid(authEid)
                .setAuthMethod(authMethod);

        if (subjectBirthYear == null && subjectAgeAtEvent == null && eventSubjectPid != null ) {
            LocalDate dateOfBirth = computeDOB(eventSubjectPid);
            if (dateOfBirth != null) {
                recordBuilder.setSubjectBirthyear(dateOfBirth.getYear());
                recordBuilder.setSubjectAgeAtEvent(
                        Period.between(dateOfBirth, getEventCreated().atZone(ZoneId.systemDefault())
                        .toLocalDate()).getYears());
            }
        }

        return recordBuilder.build();
    }

}
