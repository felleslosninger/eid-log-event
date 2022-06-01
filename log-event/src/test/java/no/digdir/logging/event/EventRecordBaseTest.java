package no.digdir.logging.event;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventRecordBaseTest {


    @Test
    void eventCreatedOnConstructionWhenNotProvided() {
        ActivityRecord activityRecord = ActivityRecord.builder().eventName("eventName").build();
        assertNotNull(activityRecord.getEventCreated());
        assertTrue(activityRecord.getEventCreated().isAfter(Instant.now().minus(Duration.ofSeconds(1))));
    }

    @Test
    void eventCreatedWhenProvided() {
        Instant eventCreated = Instant.now().minus(Duration.ofSeconds(50));
        ActivityRecord activityRecord = ActivityRecord.builder()
                .eventName("eventName")
                .eventCreated(eventCreated)
                .build();
        assertNotNull(activityRecord.getEventCreated());
        assertEquals(eventCreated, activityRecord.getEventCreated());
    }
}