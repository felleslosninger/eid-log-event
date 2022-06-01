package no.digdir.logging.event;

import lombok.Builder;
import lombok.Getter;
import no.digdir.logging.event.generated.MaskinPortenTokenIssuedAvro;
import no.digdir.logging.event.generated.TokenScopeAvro;
import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class MPTokenIssuedRecord extends EventRecordBase {

    private final String clientId;
    private final String clientOrgno;
    private final String clientOnBehalfOfId;
    private final String tokenIss;
    private final Integer tokenLifetimeSeconds;
    private final List<TokenScope> tokenScopes;
    private final String supplier;
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

    @Override
    protected SpecificRecordBase toAvroObject() {
        return MaskinPortenTokenIssuedAvro.newBuilder()
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
                .setTokenIss(tokenIss)
                .setTokenLifetimeSeconds(tokenLifetimeSeconds)
                .setTokenScopes(
                        tokenScopes == null ? null :
                                tokenScopes.stream()
                                        .map(tokenScope -> TokenScopeAvro.newBuilder()
                                                .setScope(tokenScope.scope)
                                                .setDelegationSource(tokenScope.delegationSource)
                                                .build())
                                        .collect(Collectors.toList())
                )
                .setSupplier(supplier)
                .setConsumer(consumer)
                .build();
    }

    static class TokenScope {
        private final String scope;
        private final String delegationSource;

        TokenScope(String scope, String delegationSource) {
            this.scope = scope;
            this.delegationSource = delegationSource;
        }

        String getScope() {
            return scope;
        }

        String getDelegationSource() {
            return delegationSource;
        }
    }
}
