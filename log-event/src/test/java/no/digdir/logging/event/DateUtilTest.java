package no.digdir.logging.event;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilTest {

    @ParameterizedTest
    @CsvSource({
            "25079418415, 1994-07-25",
            "28901218287, 1912-10-28",
            "14890483721, 2004-09-14",
            "09909774591, 1897-10-09",
    })
    void computeDOB(String pid, LocalDate dateOfBirth) {
        assertEquals(dateOfBirth, DateUtil.computeDOB(pid).get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000",
            "12345678901",
            "1234567890123",
            "123",
            ""
    })
    void computeDOB_shouldReturnEmtpy(String pid) {
        assertEquals(Optional.empty(), DateUtil.computeDOB(pid));
    }
}