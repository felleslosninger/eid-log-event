package no.digdir.logging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
public class MPTokenIssuedRecord extends EventRecordBase {

    @JsonProperty("client_id")
    private final String clientId;
    @JsonProperty("client_orgno")
    private final String clientOrgno;
    @JsonProperty("client_on_behalf_of_id")
    private final String clientOnBehalfOfId;
    @JsonProperty("token_iss")
    private final String tokenIss;
    @JsonProperty("token_lifetime_seconds")
    private final Integer tokenLifetimeSeconds;
    @JsonProperty("token_scopes")
    private final List<TokenScope> tokenScopes;
    @JsonProperty("supplier")
    private final String supplier;
    @JsonProperty("consumer")
    private final String consumer;

    @Builder
    protected MPTokenIssuedRecord(
            String eventName,
            String eventDescription,
            String correlationId,
            Map<String, String> extraData,
            String clientId,
            String clientOrgno,
            String clientOnBehalfOfId,
            String tokenIss,
            Integer tokenLifetimeSeconds,
            List<TokenScope> tokenScopes,
            String supplier,
            String consumer,
            Instant eventCreated) {
        super(eventName, eventDescription, correlationId, extraData, eventCreated);
        this.clientId = clientId;
        this.clientOrgno = clientOrgno;
        this.clientOnBehalfOfId = clientOnBehalfOfId;
        this.tokenIss = tokenIss;
        this.tokenLifetimeSeconds = tokenLifetimeSeconds;
        this.tokenScopes = tokenScopes;
        this.supplier = supplier;
        this.consumer = consumer;
    }

    @Getter
    public static class TokenScope {
        @JsonProperty("scope")
        private final String scope;
        @JsonProperty("delegation_source")
        private final String delegationSource;

        @Builder
        public TokenScope(String scope, String delegationSource) {
            this.scope = scope;
            this.delegationSource = delegationSource;
        }
    }
}
