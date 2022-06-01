package no.digdir.logging.event;

import lombok.Builder;
import lombok.Getter;
import no.digdir.logging.event.generated.MaskinportenAuthenticationAvro;
import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class MPAuthenticationRecord extends EventRecordBase {

    private final String clientId;
    private final String clientOrgno;
    private final String clientOnBehalfOfId;
    private final String certificateIssuer;
    private final String certificateSerialNumber;
    private final String kid;
    private final String aud;
    private final String tokenEndpointAuthMethod;

    @Builder
    public MPAuthenticationRecord(
            String eventName,
            String eventDescription,
            String correlationId,
            Map<String, String> extraData,
            String clientId,
            String clientOrgno,
            String clientOnBehalfOfId,
            String certificateIssuer,
            String certificateSerialNumber,
            String kid,
            String aud,
            String tokenEndpointAuthMethod,
            Instant eventCreated) {
        super(eventName, eventDescription, correlationId, extraData, eventCreated);
        this.clientId = clientId;
        this.clientOrgno = clientOrgno;
        this.clientOnBehalfOfId = clientOnBehalfOfId;
        this.certificateIssuer = certificateIssuer;
        this.certificateSerialNumber = certificateSerialNumber;
        this.kid = kid;
        this.aud = aud;
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    @Override
    protected SpecificRecordBase toAvroObject() {
        return MaskinportenAuthenticationAvro.newBuilder()
                .setEventName(getEventName())
                .setEventDescription(getEventDescription())
                .setEventCreated(getEventCreated())
                .setApplicationEnvironment(getApplicationEnvironment())
                .setApplicationName(getApplicationName())
                .setCorrelationId(getCorrelationId())
                .setExtraData(getExtraData() == null ? null : getExtraData().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .setClientId(clientId)
                .setClientOrgno(clientOrgno)
                .setClientOnBehalfOfId(clientOnBehalfOfId)
                .setCertificateIssuer(certificateIssuer)
                .setCertificateSerialNumber(certificateSerialNumber)
                .setKid(kid)
                .setAud(aud)
                .setTokenEndpointAuthMethod(tokenEndpointAuthMethod)
                .build();
    }
}
