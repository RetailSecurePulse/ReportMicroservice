package com.retailpulse.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilTest {

    @Test
    void convertStringToInstant_usesProvidedFormatAndSingaporeOffset() {
        Instant result = DateUtil.convertStringToInstant("2024-01-02 08:15:30", "yyyy-MM-dd HH:mm:ss");

        assertEquals(Instant.parse("2024-01-02T00:15:30Z"), result);
    }

    @Test
    void convertInstantToString_formatsInSingaporeTimezone() {
        String result = DateUtil.convertInstantToString(
                Instant.parse("2024-01-02T00:15:30Z"),
                "yyyy-MM-dd HH:mm:ss"
        );

        assertEquals("2024-01-02 08:15:30", result);
    }
}
