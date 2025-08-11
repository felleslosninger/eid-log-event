package no.digdir.logging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
public class MPAuthenticationRecord extends EventRecordBase {

    @JsonProperty("client_id")
    private final String clientId;
    @JsonProperty("client_orgno")
    private final String clientOrgno;
    @JsonProperty("client_on_behalf_of_id")
    private final String clientOnBehalfOfId;
    @JsonProperty("certificate_issuer")
    private final String certificateIssuer;
    @JsonProperty("certificate_serial_number")
    private final String certificateSerialNumber;
    @JsonProperty("kid")
    private final String kid;
    @JsonProperty("aud")
    private final String aud;
    @JsonProperty("token_endpoint_auth_method")
    private final String tokenEndpointAuthMethod;
    @JsonProperty("consumer")
    private final String consumer;

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
            String consumer,
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
        this.consumer = consumer;
    }
}
