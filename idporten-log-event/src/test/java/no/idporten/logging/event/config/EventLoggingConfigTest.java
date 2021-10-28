package no.idporten.logging.event.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventLoggingConfigTest {

    private static final String KEY = "key";
    private static final String ORIGINAL_VALUE = "old";
    private static final String NEW_VALUE = "new";

    @Test
    void replaceIfSet() {
        assertEquals(ORIGINAL_VALUE, EventLoggingConfig.replaceIfSet(null).apply(KEY, ORIGINAL_VALUE));
        assertEquals(NEW_VALUE, EventLoggingConfig.replaceIfSet(NEW_VALUE).apply(KEY, ORIGINAL_VALUE));
        assertEquals("", EventLoggingConfig.replaceIfSet("").apply(KEY, ORIGINAL_VALUE));
    }
}