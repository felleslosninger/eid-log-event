package no.digdir.logging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static no.digdir.logging.event.DateUtil.computeDOB;

@Getter
public class ActivityRecord extends EventRecordBase {

    @JsonProperty("event_actor_id")
    private final String eventActorId;
    @JsonProperty("event_subject_pid")
    private final String eventSubjectPid;
    @JsonProperty("service_provider_id")
    private final String serviceProviderId;
    @JsonProperty("service_provider_orgno")
    private final String serviceProviderOrgno;
    @JsonProperty("service_provider_name")
    private final String serviceProviderName;
    @JsonProperty("service_owner_id")
    private final String serviceOwnerId;
    @JsonProperty("service_owner_orgno")
    private final String serviceOwnerOrgno;
    @JsonProperty("service_owner_name")
    private final String serviceOwnerName;
    @JsonProperty("auth_eid")
    private final String authEid;
    @JsonProperty("auth_method")
    private final String authMethod;
    @JsonProperty("subject_birthyear")
    private  final Integer subjectBirthYear;
    @JsonProperty("subject_age_at_event")
    private final Integer subjectAgeAtEvent;
    @JsonProperty("user_agent")
    private final String userAgent;
    @JsonProperty("user_ip")
    private final String userIp;

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
            Instant eventCreated,
            String userAgent,
            String userIp) {
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
        this.userAgent = userAgent;
        this.userIp = userIp;
        Optional<LocalDate> dateOfBirth = computeDOB(this.eventSubjectPid);
        if (dateOfBirth.isPresent()) {
            this.subjectBirthYear = dateOfBirth.get().getYear();
            this.subjectAgeAtEvent =
                    Period.between(dateOfBirth.orElse(null), getEventCreated().atZone(ZoneId.systemDefault())
                            .toLocalDate()).getYears();
        } else {
            this.subjectBirthYear = null;
            this.subjectAgeAtEvent = null;
        }
    }
}
