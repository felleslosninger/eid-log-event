package no.idporten.logging.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventRecordTest {

    private static final String FNR = "25079418415";

    @Test
    void getApplication() {
        String name = "unitTest";

        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setApplication(name)
                .build();

        assertEquals(name, record.getApplication());
    }

    @Test
    void getEnvironment() {
        String environment = "unitTest";

        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setEnvironment(environment)
                .build();

        assertEquals(environment, record.getEnvironment());
    }

    @Test
    void getDescription() {
        String description = "Brukeren har logget inn";

        EventRecord record = EventRecord.newBuilder()
                .setName("Innlogget")
                .setPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setDescription(description)
                .build();

        assertEquals(description, record.getDescription());
    }
}