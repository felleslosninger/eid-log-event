package no.digdir.logging.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityRecordTest {

    private static final String FNR = "25079418415";

    @Test
    void getApplication() {
        String name = "unitTest";

        ActivityRecord record = ActivityRecord.newBuilder()
                .setEventName("Innlogget")
                .setEventSubjectPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setApplicationName(name)
                .build();

        assertEquals(name, record.getApplicationName());
    }

    @Test
    void getEnvironment() {
        String environment = "unitTest";

        ActivityRecord record = ActivityRecord.newBuilder()
                .setEventName("Innlogget")
                .setEventSubjectPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setApplicationEnvironment(environment)
                .build();

        assertEquals(environment, record.getApplicationEnvironment());
    }

    @Test
    void getDescription() {
        String description = "Brukeren har logget inn";

        ActivityRecord record = ActivityRecord.newBuilder()
                .setEventName("Innlogget")
                .setEventSubjectPid(FNR)
                .setCorrelationId(UUID.randomUUID().toString())
                .setEventDescription(description)
                .build();

        assertEquals(description, record.getEventDescription());
    }
}